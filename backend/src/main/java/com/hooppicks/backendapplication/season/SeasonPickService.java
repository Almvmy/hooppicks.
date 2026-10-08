package com.hooppicks.backendapplication.season;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pronostics de saison : quelques questions sur toute la saison (champion,
 * finalistes, meilleur bilan), une équipe par réponse, verrouillées une
 * semaine après le premier match de saison régulière. Les bonnes réponses sont saisies par un
 * admin (console) : plus fiable que de déduire la fin des playoffs des
 * données de matchs. Classement à part, qui ne touche pas au classement des
 * semaines.
 */
@Service
public class SeasonPickService {

    public record Question(String key, String label, int points) {}

    public static final List<Question> QUESTIONS = List.of(
            new Question("champion", "Champion NBA", 300),
            new Question("east_finalist", "Finaliste de l'Est", 150),
            new Question("west_finalist", "Finaliste de l'Ouest", 150),
            new Question("best_record", "Meilleur bilan de la saison régulière", 100)
    );

    private final SeasonPickRepository pickRepository;
    private final SeasonResultRepository resultRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public SeasonPickService(SeasonPickRepository pickRepository, SeasonResultRepository resultRepository,
                             MatchRepository matchRepository, TeamRepository teamRepository,
                             UserRepository userRepository) {
        this(pickRepository, resultRepository, matchRepository, teamRepository, userRepository,
                Clock.system(BankrollService.ZONE));
    }

    SeasonPickService(SeasonPickRepository pickRepository, SeasonResultRepository resultRepository,
                      MatchRepository matchRepository, TeamRepository teamRepository,
                      UserRepository userRepository, Clock clock) {
        this.pickRepository = pickRepository;
        this.resultRepository = resultRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public static class SeasonPickException extends RuntimeException {
        public SeasonPickException(String message) {
            super(message);
        }
    }

    /** « 2026-27 » : une saison NBA commence en octobre, on bascule au 1er septembre. */
    public String currentSeason() {
        LocalDate today = LocalDate.now(clock);
        int start = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return start + "-" + String.valueOf(start + 1).substring(2);
    }

    // Choix du propriétaire : une semaine de rab après le coup d'envoi, pour
    // laisser aux joueurs arrivés avec le lancement le temps de pronostiquer.
    static final Duration GRACE_AFTER_OPENER = Duration.ofDays(7);

    /** Fin des pronostics : premier match de saison régulière + une semaine. Null tant qu'aucun n'est connu. */
    public Instant deadline(String season) {
        int start = Integer.parseInt(season.substring(0, 4));
        Instant from = LocalDate.of(start, 9, 1).atStartOfDay(BankrollService.ZONE).toInstant();
        return matchRepository.findFirstByTypeAndDateAfterOrderByDateAsc(MatchType.REGULAR, from)
                .map(m -> m.getDate().plus(GRACE_AFTER_OPENER)).orElse(null);
    }

    private boolean locked(String season) {
        Instant deadline = deadline(season);
        return deadline != null && !clock.instant().isBefore(deadline);
    }

    @Transactional
    public void pick(String userId, String question, String teamAbbreviation) {
        if (QUESTIONS.stream().noneMatch(q -> q.key().equals(question))) throw new SeasonPickException("Question inconnue.");
        if (teamRepository.findAll().stream().noneMatch(t -> t.getAbbreviation().equals(teamAbbreviation))) {
            throw new SeasonPickException("Équipe inconnue.");
        }
        String season = currentSeason();
        if (locked(season)) throw new SeasonPickException("Les pronostics de saison sont clos.");
        SeasonPick pick = pickRepository.findBySeasonAndUserIdAndQuestion(season, userId, question).orElseGet(SeasonPick::new);
        pick.setUserId(userId);
        pick.setSeason(season);
        pick.setQuestion(question);
        pick.setTeamAbbreviation(teamAbbreviation);
        pick.setUpdatedAt(clock.instant());
        pickRepository.save(pick);
    }

    @Transactional
    public void setResult(String question, String teamAbbreviation) {
        if (QUESTIONS.stream().noneMatch(q -> q.key().equals(question))) throw new SeasonPickException("Question inconnue.");
        String season = currentSeason();
        SeasonResult result = resultRepository.findBySeasonAndQuestion(season, question).orElseGet(SeasonResult::new);
        result.setSeason(season);
        result.setQuestion(question);
        result.setTeamAbbreviation(teamAbbreviation);
        result.setDecidedAt(clock.instant());
        resultRepository.save(result);
    }

    public record Standing(String username, long points, int correct) {}

    public record Overview(String season, String deadline, boolean locked, List<Question> questions,
                           Map<String, String> myPicks, Map<String, String> results,
                           /** Après le verrou seulement : sinon on copierait les choix des autres. */
                           Map<String, Map<String, Integer>> community,
                           List<Standing> leaderboard) {}

    @Transactional(readOnly = true)
    public Overview overview(String userId) {
        String season = currentSeason();
        Instant deadline = deadline(season);
        boolean locked = deadline != null && !clock.instant().isBefore(deadline);
        List<SeasonPick> all = pickRepository.findBySeason(season);

        Map<String, String> mine = all.stream().filter(p -> p.getUserId().equals(userId))
                .collect(Collectors.toMap(SeasonPick::getQuestion, SeasonPick::getTeamAbbreviation));
        Map<String, String> results = resultRepository.findBySeason(season).stream()
                .collect(Collectors.toMap(SeasonResult::getQuestion, SeasonResult::getTeamAbbreviation));

        Map<String, Map<String, Integer>> community = new HashMap<>();
        if (locked) {
            Map<String, List<SeasonPick>> byQuestion = all.stream().collect(Collectors.groupingBy(SeasonPick::getQuestion));
            byQuestion.forEach((q, picks) -> {
                Map<String, Long> counts = picks.stream().collect(Collectors.groupingBy(SeasonPick::getTeamAbbreviation, Collectors.counting()));
                Map<String, Integer> pct = new LinkedHashMap<>();
                counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .forEach(e -> pct.put(e.getKey(), (int) Math.round(e.getValue() * 100.0 / picks.size())));
                community.put(q, pct);
            });
        }

        List<Standing> leaderboard = results.isEmpty() ? List.of() : standings(all, results);
        return new Overview(season, deadline == null ? null : deadline.toString(), locked, QUESTIONS, mine, results,
                community, leaderboard);
    }

    private List<Standing> standings(List<SeasonPick> picks, Map<String, String> results) {
        Map<String, Integer> pointsByQuestion = QUESTIONS.stream().collect(Collectors.toMap(Question::key, Question::points));
        Map<String, long[]> byUser = new HashMap<>();
        for (SeasonPick p : picks) {
            long[] acc = byUser.computeIfAbsent(p.getUserId(), k -> new long[2]);
            if (p.getTeamAbbreviation().equals(results.get(p.getQuestion()))) {
                acc[0] += pointsByQuestion.get(p.getQuestion());
                acc[1]++;
            }
        }
        Map<String, String> names = userRepository.findAllById(byUser.keySet()).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        return byUser.entrySet().stream()
                .filter(e -> names.containsKey(e.getKey()))
                .map(e -> new Standing(names.get(e.getKey()), e.getValue()[0], (int) e.getValue()[1]))
                .sorted(Comparator.comparingLong(Standing::points).reversed().thenComparing(Standing::username))
                .toList();
    }
}
