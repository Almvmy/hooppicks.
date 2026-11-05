package com.hooppicks.backendapplication.quiz;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.chat.NightThreadService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le quiz du jour : 5 questions, les mêmes pour tous, un seul essai, 15 secondes
 * par question. Le score se compte en paniers, pas en points (les points sont ceux
 * des paris), et ne touche à rien d'autre dans le jeu. Classement unique, repart de
 * zéro chaque lundi (choix du propriétaire) ; seuls les 5 meilleurs jours de la
 * semaine comptent, pour qu'un joueur qui rate deux jours reste dans la course. À
 * égalité de paniers, le plus rapide passe devant.
 *
 * Tout se corrige ici : les questions partent une par une et sans la réponse, et
 * c'est l'heure du serveur qui fait foi. Revenir sur l'app après avoir quitté une
 * question la compte ratée : on ne gagne pas de temps en sortant chercher.
 */
@Service
public class QuizService {

    static final Duration LIMIT = Duration.ofSeconds(15);
    // Le temps que la réponse arrive, sur une connexion mobile lente.
    static final Duration GRACE = Duration.ofSeconds(2);
    // La réponse s'affiche un instant à l'écran : le chrono suivant démarre après.
    static final Duration FEEDBACK = Duration.ofMillis(1500);
    static final int COUNTED_DAYS = 5;
    private static final int BOARD_SIZE = 50;

    public record QuestionDto(int index, int total, String text, String imageUrl, List<String> choices, double secondsLeft) {}

    public record ReviewDto(String text, String answer, boolean hit) {}

    public record Standing(int rank, int players, int baskets, int days, String level) {}

    /** `status` : ready (pas joué), playing, done. */
    public record QuizState(LocalDate day, String status, String shots, QuestionDto question, List<ReviewDto> review, Standing standing) {}

    public record AnswerResult(boolean hit, int correct, QuizState state) {}

    public record BoardEntry(int rank, String username, Integer avatarNumber, String avatarPosition, String avatarColorway,
                             String avatarIcon, int baskets, int days, long seconds, String level, boolean me) {}

    public record LastChampion(String username, int baskets, LocalDate week) {}

    public record Board(LocalDate week, List<BoardEntry> entries, BoardEntry me, LastChampion lastChampion) {}

    private record Score(String userId, int baskets, int days, long millis) {}

    private final QuizQuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizChampionRepository championRepository;
    private final RosterPlayerRepository rosterRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final PlayerMatchStatRepository statRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final TransactionOperations tx;
    private final Clock clock;

    @Autowired
    public QuizService(QuizQuestionRepository questionRepository, QuizAttemptRepository attemptRepository,
                       QuizChampionRepository championRepository, RosterPlayerRepository rosterRepository,
                       TeamRepository teamRepository, MatchRepository matchRepository, PlayerMatchStatRepository statRepository,
                       UserRepository userRepository, NotificationRepository notificationRepository, PushService pushService,
                       TransactionOperations tx) {
        this(questionRepository, attemptRepository, championRepository, rosterRepository, teamRepository, matchRepository,
                statRepository, userRepository, notificationRepository, pushService, tx, Clock.system(BankrollService.ZONE));
    }

    QuizService(QuizQuestionRepository questionRepository, QuizAttemptRepository attemptRepository,
                QuizChampionRepository championRepository, RosterPlayerRepository rosterRepository,
                TeamRepository teamRepository, MatchRepository matchRepository, PlayerMatchStatRepository statRepository,
                UserRepository userRepository, NotificationRepository notificationRepository, PushService pushService,
                TransactionOperations tx, Clock clock) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.championRepository = championRepository;
        this.rosterRepository = rosterRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.statRepository = statRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.tx = tx;
        this.clock = clock;
    }

    private LocalDate today() {
        return NightThreadService.nightOf(clock.instant());
    }

    static LocalDate weekOf(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    static String level(int baskets) {
        if (baskets >= 22) return "MVP";
        if (baskets >= 15) return "All-Star";
        if (baskets >= 8) return "Titulaire";
        return "Rookie";
    }

    // --- Questions du jour ---

    // Hors de la transaction de la partie : un conflit entre deux joueurs arrivés
    // ensemble à midi n'annule que cette écriture, le second relit celle du premier.
    private List<QuizQuestion> ensureQuestions(LocalDate day) {
        List<QuizQuestion> existing = questionRepository.findByDayOrderByPositionAsc(day);
        if (!existing.isEmpty()) return existing;
        QuizGenerator.Facts facts = tx.execute(s -> readFacts(day));
        List<QuizGenerator.Question> generated = QuizGenerator.generate(day, facts);
        List<QuizQuestion> rows = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            QuizGenerator.Question g = generated.get(i);
            QuizQuestion q = new QuizQuestion();
            q.setId(day + ":" + i);
            q.setDay(day);
            q.setPosition(i);
            q.setText(g.text());
            q.setImageUrl(g.imageUrl());
            q.setChoices(String.join("\n", g.choices()));
            q.setCorrect(g.correct());
            rows.add(q);
        }
        try {
            questionRepository.saveAll(rows);
        } catch (DataIntegrityViolationException e) {
            return questionRepository.findByDayOrderByPositionAsc(day);
        }
        return rows;
    }

    private QuizGenerator.Facts readFacts(LocalDate day) {
        Instant to = day.atTime(LocalTime.NOON).atZone(BankrollService.ZONE).toInstant();
        Instant from = to.minus(Duration.ofDays(1));
        List<QuizGenerator.PlayerFact> players = rosterRepository.findAllWithTeam().stream()
                .map(r -> new QuizGenerator.PlayerFact(fullName(r), r.getTeam() == null ? null : r.getTeam().getName(), r.getJersey(),
                        r.getHeadshotUrl(), r.getMinutesPerGame(), r.getGamesPlayed(), r.getPointsPerGame(), r.getReboundsPerGame(), r.getAssistsPerGame()))
                .toList();
        List<QuizGenerator.TeamFact> teams = teamRepository.findAll().stream()
                .map(t -> new QuizGenerator.TeamFact(t.getName(), t.getConference(), t.getWins(), t.getLosses()))
                .toList();
        List<QuizGenerator.GameFact> games = matchRepository.findByStatusAndDateBetween(MatchStatus.FINISHED, from, to).stream()
                .filter(m -> m.getHomeScore() != null && m.getAwayScore() != null)
                .map(m -> new QuizGenerator.GameFact(m.getAwayTeam().getAbbreviation(), m.getHomeTeam().getAbbreviation(), m.getAwayScore(), m.getHomeScore()))
                .toList();
        List<QuizGenerator.NightLine> lines = statRepository.findFinishedBetween(from, to).stream()
                .map(s -> new QuizGenerator.NightLine(s.getPlayerName(), s.getPoints()))
                .toList();
        return new QuizGenerator.Facts(players, teams, games, lines);
    }

    private static String fullName(RosterPlayer r) {
        return ((r.getFirstName() == null ? "" : r.getFirstName()) + " " + (r.getLastName() == null ? "" : r.getLastName())).trim();
    }

    // --- La partie ---

    private static String attemptId(String userId, LocalDate day) {
        return userId + ":" + day;
    }

    public QuizState state(String userId) {
        LocalDate day = today();
        List<QuizQuestion> questions = ensureQuestions(day);
        return tx.execute(s -> {
            QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId(userId, day)).orElse(null);
            if (attempt != null) expireIfLate(attempt, questions);
            return toState(userId, day, attempt, questions);
        });
    }

    public QuizState start(String userId) {
        LocalDate day = today();
        List<QuizQuestion> questions = ensureQuestions(day);
        try {
            return tx.execute(s -> {
                QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId(userId, day)).orElse(null);
                if (attempt == null) {
                    attempt = new QuizAttempt();
                    attempt.setId(attemptId(userId, day));
                    attempt.setUserId(userId);
                    attempt.setDay(day);
                    attempt.setWeek(weekOf(day));
                    attempt.setShots("");
                    attempt.setQuestionStartedAt(clock.instant());
                    attemptRepository.save(attempt);
                }
                return toState(userId, day, attempt, questions);
            });
        } catch (DataIntegrityViolationException e) {
            return state(userId); // Double appui sur « Jouer » : la partie existe déjà.
        }
    }

    /** `choice` null : temps écoulé. `index` : la question à laquelle l'écran répond, un doublon est ignoré. */
    public AnswerResult answer(String userId, int index, Integer choice) {
        LocalDate day = today();
        List<QuizQuestion> questions = ensureQuestions(day);
        return tx.execute(s -> {
            QuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId(userId, day))
                    .orElseThrow(() -> new IllegalArgumentException("Partie introuvable."));
            int current = attempt.getShots().length();
            if (index != current || current >= questions.size()) {
                return new AnswerResult(false, index >= 0 && index < questions.size() ? questions.get(index).getCorrect() : -1, toState(userId, day, attempt, questions));
            }
            QuizQuestion question = questions.get(current);
            Duration elapsed = Duration.between(attempt.getQuestionStartedAt(), clock.instant());
            if (elapsed.isNegative()) elapsed = Duration.ZERO;
            boolean inTime = elapsed.compareTo(LIMIT.plus(GRACE)) <= 0;
            boolean hit = inTime && choice != null && choice == question.getCorrect();
            record(attempt, hit, hit ? Math.min(elapsed.toMillis(), LIMIT.toMillis()) : LIMIT.toMillis());
            return new AnswerResult(hit, question.getCorrect(), toState(userId, day, attempt, questions));
        });
    }

    private void record(QuizAttempt attempt, boolean hit, long millis) {
        attempt.setShots(attempt.getShots() + (hit ? "1" : "0"));
        attempt.setTotalMillis(attempt.getTotalMillis() + millis);
        attempt.setQuestionStartedAt(clock.instant().plus(FEEDBACK));
    }

    // Une question laissée en plan est ratée, une seule à la fois : la suivante repart avec son chrono.
    private void expireIfLate(QuizAttempt attempt, List<QuizQuestion> questions) {
        if (attempt.getShots().length() >= questions.size()) return;
        if (Duration.between(attempt.getQuestionStartedAt(), clock.instant()).compareTo(LIMIT.plus(GRACE)) > 0) {
            record(attempt, false, LIMIT.toMillis());
            attempt.setQuestionStartedAt(clock.instant());
        }
    }

    private QuizState toState(String userId, LocalDate day, QuizAttempt attempt, List<QuizQuestion> questions) {
        Standing standing = standing(userId, weekOf(day));
        if (attempt == null) return new QuizState(day, "ready", "", null, null, standing);
        String shots = attempt.getShots();
        if (shots.length() >= questions.size()) {
            List<ReviewDto> review = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                review.add(new ReviewDto(q.getText(), choicesOf(q).get(q.getCorrect()), shots.charAt(i) == '1'));
            }
            return new QuizState(day, "done", shots, null, review, standing);
        }
        QuizQuestion q = questions.get(shots.length());
        long left = Duration.between(clock.instant(), attempt.getQuestionStartedAt().plus(LIMIT)).toMillis();
        double seconds = Math.max(0, Math.min(LIMIT.toMillis(), left)) / 1000.0;
        return new QuizState(day, "playing", shots,
                new QuestionDto(shots.length(), questions.size(), q.getText(), q.getImageUrl(), choicesOf(q), seconds), null, standing);
    }

    private static List<String> choicesOf(QuizQuestion q) {
        return List.of(q.getChoices().split("\n"));
    }

    // --- Classement ---

    private static int hits(QuizAttempt a) {
        return (int) a.getShots().chars().filter(c -> c == '1').count();
    }

    private List<Score> ranking(LocalDate week) {
        Map<String, List<QuizAttempt>> byUser = attemptRepository.findByWeek(week).stream()
                .filter(a -> !a.getShots().isEmpty())
                .collect(Collectors.groupingBy(QuizAttempt::getUserId));
        return byUser.entrySet().stream().map(e -> {
            List<QuizAttempt> best = e.getValue().stream()
                    .sorted(Comparator.comparingInt(QuizService::hits).reversed().thenComparingLong(QuizAttempt::getTotalMillis))
                    .limit(COUNTED_DAYS).toList();
            return new Score(e.getKey(), best.stream().mapToInt(QuizService::hits).sum(), e.getValue().size(),
                    best.stream().mapToLong(QuizAttempt::getTotalMillis).sum());
        }).sorted(Comparator.comparingInt(Score::baskets).reversed().thenComparingLong(Score::millis)).toList();
    }

    private Standing standing(String userId, LocalDate week) {
        List<Score> ranking = ranking(week);
        for (int i = 0; i < ranking.size(); i++) {
            Score s = ranking.get(i);
            if (s.userId().equals(userId)) return new Standing(i + 1, ranking.size(), s.baskets(), s.days(), level(s.baskets()));
        }
        return new Standing(0, ranking.size(), 0, 0, level(0));
    }

    public Board board(String userId) {
        LocalDate week = weekOf(today());
        List<Score> ranking = ranking(week);
        List<Score> shown = new ArrayList<>(ranking.subList(0, Math.min(BOARD_SIZE, ranking.size())));
        Set<String> ids = shown.stream().map(Score::userId).collect(Collectors.toSet());
        ids.add(userId);
        Map<String, User> users = userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<BoardEntry> entries = new ArrayList<>();
        for (int i = 0; i < shown.size(); i++) {
            BoardEntry entry = entry(i + 1, shown.get(i), users.get(shown.get(i).userId()), userId);
            if (entry != null) entries.add(entry);
        }
        BoardEntry me = null;
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).userId().equals(userId)) me = entry(i + 1, ranking.get(i), users.get(userId), userId);
        }
        LastChampion champion = championRepository.findById(week.minusWeeks(1))
                .map(c -> new LastChampion(c.getUsername(), c.getBaskets(), c.getWeek())).orElse(null);
        return new Board(week, entries, me, champion);
    }

    private static BoardEntry entry(int rank, Score s, User u, String viewerId) {
        if (u == null) return null;
        return new BoardEntry(rank, u.getUsername(), u.getAvatarNumber(), u.getAvatarPosition(), u.getAvatarColorway(), u.getAvatarIcon(),
                s.baskets(), s.days(), Math.round(s.millis() / 1000.0), level(s.baskets()), u.getId().equals(viewerId));
    }

    /** La semaine vient de finir : son champion, une seule fois. Lancé chaque minute par BankrollScheduler. */
    public void recordEndedWeek() {
        LocalDate lastWeek = weekOf(today()).minusWeeks(1);
        if (championRepository.existsById(lastWeek)) return;
        List<Score> ranking = ranking(lastWeek);
        if (ranking.isEmpty() || ranking.get(0).baskets() == 0) return;
        Score best = ranking.get(0);
        User user = userRepository.findById(best.userId()).orElse(null);
        if (user == null) return;
        QuizChampion champion = new QuizChampion();
        champion.setWeek(lastWeek);
        champion.setUserId(user.getId());
        champion.setUsername(user.getUsername());
        champion.setBaskets(best.baskets());
        championRepository.save(champion);

        String message = "Champion du quiz de la semaine, avec " + best.baskets() + " paniers !";
        AppNotification n = new AppNotification();
        n.setUser(user);
        n.setType(NotificationType.SOCIAL);
        n.setMessage(message);
        n.setLink("/quiz");
        notificationRepository.save(n);
        if (user.isNotifyLeagueActivity()) pushService.sendToUser(user.getId(), new PushService.PushMessage("Quiz HoopPicks", message, "/quiz"));
    }
}
