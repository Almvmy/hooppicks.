package com.hooppicks.backendapplication.bracket;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le tableau à remplir : un vainqueur par match, du premier tour à la finale, sans
 * mise. Classement à part, qui ne touche ni aux points ni au solde. Les 8 équipes
 * sont saisies dans la console une fois les poules finies (ni balldontlie ni ESPN
 * ne donnent le tableau de façon fiable). Les vainqueurs se lisent dans les matchs
 * de Coupe terminés ; un admin peut les saisir si la source tarde ou se trompe.
 *
 * Ce qu'ont choisi les autres reste caché jusqu'au verrou (sinon on copierait),
 * comme les pronostics de saison. Le verrou tombe au plus tôt : si le premier match
 * réel est avant l'heure saisie, c'est lui qui compte.
 */
@Service
public class BracketService {

    private static final int RANKING_SIZE = 50;
    private static final Duration REMINDER_BEFORE = Duration.ofHours(24);
    static final String PHASE = "BRACKET";

    public record GameDto(int index, int round, int points, String teamA, String teamB, String winner) {}

    public record RankingEntry(int rank, String username, Integer avatarNumber, String avatarPosition, String avatarColorway,
                               String avatarIcon, int points, String champion, boolean me) {}

    public record ChampionShare(String team, int players) {}

    public record Overview(String id, String label, Instant locksAt, boolean locked, int teams, List<GameDto> games, List<String> myPicks,
                           Integer myPoints, long players, List<RankingEntry> ranking, List<ChampionShare> champions) {}

    public static class BracketException extends RuntimeException {
        public BracketException(String message) {
            super(message);
        }
    }

    private final BracketRepository bracketRepository;
    private final BracketPickRepository pickRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final PhaseTitleRepository titleRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final Clock clock;

    @Autowired
    public BracketService(BracketRepository bracketRepository, BracketPickRepository pickRepository, TeamRepository teamRepository,
                          MatchRepository matchRepository, UserRepository userRepository, PhaseTitleRepository titleRepository,
                          NotificationRepository notificationRepository, PushService pushService) {
        this(bracketRepository, pickRepository, teamRepository, matchRepository, userRepository, titleRepository,
                notificationRepository, pushService, Clock.system(BankrollService.ZONE));
    }

    BracketService(BracketRepository bracketRepository, BracketPickRepository pickRepository, TeamRepository teamRepository,
                   MatchRepository matchRepository, UserRepository userRepository, PhaseTitleRepository titleRepository,
                   NotificationRepository notificationRepository, PushService pushService, Clock clock) {
        this.bracketRepository = bracketRepository;
        this.pickRepository = pickRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.titleRepository = titleRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.clock = clock;
    }

    private boolean locked(Bracket b) {
        return !clock.instant().isBefore(b.getLocksAt());
    }

    private static String pickId(String userId, String bracketId) {
        return userId + ":" + bracketId;
    }

    // --- Lecture ---

    @Transactional(readOnly = true)
    public Optional<Overview> current(String userId) {
        return bracketRepository.findTopByOrderByLocksAtDesc().map(b -> overview(userId, b));
    }

    private Overview overview(String userId, Bracket b) {
        List<String> teams = BracketTree.parse(b.getTeams());
        List<String> winners = BracketTree.parse(b.getWinners());
        boolean locked = locked(b);

        List<GameDto> games = new ArrayList<>();
        for (int g = 0; g < BracketTree.games(teams.size()); g++) {
            List<String> pair = BracketTree.participants(teams, winners, g);
            games.add(new GameDto(g, BracketTree.round(teams.size(), g), BracketTree.points(teams.size(), g), pair.get(0), pair.get(1), winners.get(g)));
        }
        List<BracketPick> all = pickRepository.findByBracketId(b.getId());
        List<String> mine = pickRepository.findById(pickId(userId, b.getId())).map(p -> BracketTree.parse(p.getPicks())).orElse(null);
        return new Overview(b.getId(), b.getLabel(), b.getLocksAt(), locked, teams.size(), games, mine,
                mine == null ? null : BracketTree.score(teams, winners, mine), all.size(),
                locked ? ranking(userId, teams, winners, all) : null,
                locked ? championShares(all) : null);
    }

    private List<RankingEntry> ranking(String userId, List<String> teams, List<String> winners, List<BracketPick> all) {
        List<BracketPick> sorted = all.stream()
                .sorted(Comparator.comparingInt((BracketPick p) -> -BracketTree.score(teams, winners, BracketTree.parse(p.getPicks()))))
                .toList();
        Set<String> shown = sorted.stream().limit(RANKING_SIZE).map(BracketPick::getUserId).collect(Collectors.toCollection(HashSet::new));
        shown.add(userId);
        Map<String, User> users = userRepository.findAllById(shown).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<RankingEntry> entries = new ArrayList<>();
        int rank = 0, previous = -1;
        for (int i = 0; i < sorted.size(); i++) {
            BracketPick p = sorted.get(i);
            List<String> picks = BracketTree.parse(p.getPicks());
            int points = BracketTree.score(teams, winners, picks);
            if (points != previous) rank = i + 1; // Ex æquo : même place.
            previous = points;
            boolean me = p.getUserId().equals(userId);
            User u = users.get(p.getUserId());
            if (u == null || (i >= RANKING_SIZE && !me)) continue;
            entries.add(new RankingEntry(rank, u.getUsername(), u.getAvatarNumber(), u.getAvatarPosition(), u.getAvatarColorway(),
                    u.getAvatarIcon(), points, picks.get(picks.size() - 1), me));
        }
        return entries;
    }

    private static List<ChampionShare> championShares(List<BracketPick> all) {
        Map<String, Integer> counts = new HashMap<>();
        for (BracketPick p : all) {
            List<String> picks = BracketTree.parse(p.getPicks());
            counts.merge(picks.get(picks.size() - 1), 1, Integer::sum);
        }
        return counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new ChampionShare(e.getKey(), e.getValue())).toList();
    }

    // --- Le joueur remplit son tableau ---

    @Transactional
    public Overview savePicks(String userId, List<String> picks) {
        Bracket b = bracketRepository.findTopByOrderByLocksAtDesc().orElseThrow(() -> new BracketException("Pas de tableau ouvert."));
        if (locked(b)) throw new BracketException("Le tableau est verrouillé : le premier match a commencé.");
        List<String> teams = BracketTree.parse(b.getTeams());
        if (picks == null || !BracketTree.isValid(teams, picks)) throw new BracketException("Tableau incomplet ou incohérent.");
        BracketPick pick = new BracketPick();
        pick.setId(pickId(userId, b.getId()));
        pick.setBracketId(b.getId());
        pick.setUserId(userId);
        pick.setPicks(BracketTree.format(picks));
        pickRepository.save(pick);
        return overview(userId, b);
    }

    // --- Console ---

    /** Crée le tableau ou le corrige. Les équipes ne changent plus une fois des tableaux remplis : ils n'auraient plus de sens. */
    @Transactional
    public void publish(String id, String label, List<String> teams, Instant locksAt) {
        if (id == null || id.isBlank() || label == null || label.isBlank()) throw new BracketException("Nom du tableau obligatoire.");
        if (locksAt == null) throw new BracketException("Heure du premier match obligatoire.");
        if (teams == null || teams.size() != 8 || new HashSet<>(teams).size() != 8) throw new BracketException("Il faut 8 équipes différentes.");
        Set<String> known = teamRepository.findAll().stream().map(Team::getAbbreviation).collect(Collectors.toSet());
        if (!known.containsAll(teams)) throw new BracketException("Équipe inconnue.");

        Bracket existing = bracketRepository.findById(id).orElse(null);
        String csv = String.join(",", teams);
        if (existing != null && !csv.equals(existing.getTeams()) && pickRepository.countByBracketId(id) > 0) {
            throw new BracketException("Des joueurs ont déjà rempli leur tableau : les équipes ne changent plus.");
        }
        boolean first = existing == null;
        Bracket b = first ? new Bracket() : existing;
        b.setId(id);
        b.setLabel(label.strip());
        b.setTeams(csv);
        b.setLocksAt(locksAt);
        if (first) {
            b.setCreatedAt(clock.instant());
            b.setWinners(BracketTree.format(new ArrayList<>(Collections.nCopies(BracketTree.games(teams.size()), (String) null))));
        }
        bracketRepository.save(b);
        if (first && !locked(b)) announce(b);
    }

    private void announce(Bracket b) {
        for (User user : userRepository.findAll()) {
            notify(user, "Le tableau « " + b.getLabel() + " » est ouvert : à toi de le remplir avant le premier match.", "Tableau à remplir");
        }
    }

    private void notify(User user, String message, String pushTitle) {
        AppNotification n = new AppNotification();
        n.setUser(user);
        n.setType(NotificationType.SEASON_PICKS);
        n.setMessage(message);
        n.setLink("/coupe");
        notificationRepository.save(n);
        if (user.isNotifyLeagueActivity()) pushService.sendToUser(user.getId(), new PushService.PushMessage(pushTitle, message, "/coupe"));
    }

    /** Corrige un résultat. Le titre se donne une seule fois : à corriger avant la fin, ou au journal et à la main ensuite. */
    @Transactional
    public void setWinner(int game, String team) {
        Bracket b = bracketRepository.findTopByOrderByLocksAtDesc().orElseThrow(() -> new BracketException("Pas de tableau."));
        List<String> teams = BracketTree.parse(b.getTeams());
        List<String> winners = BracketTree.parse(b.getWinners());
        if (game < 0 || game >= winners.size()) throw new BracketException("Match inconnu.");
        List<String> pair = BracketTree.participants(teams, winners, game);
        if (team != null && !pair.contains(team)) throw new BracketException("Cette équipe ne joue pas ce match.");
        b.setWinners(BracketTree.format(BracketTree.withWinner(teams, winners, game, team)));
        finishIfDone(b);
    }

    // --- Chaque minute ---

    /** Lancé par BankrollScheduler. Rien à faire une fois le titre donné. */
    @Transactional
    public void tick() {
        Bracket b = bracketRepository.findTopByOrderByLocksAtDesc().orElse(null);
        if (b == null || Boolean.TRUE.equals(b.getAwarded())) return;
        Instant now = clock.instant();
        List<String> teams = BracketTree.parse(b.getTeams());
        if (!locked(b)) {
            lockAtFirstRealGame(b, teams);
        }
        if (!locked(b)) {
            if (!Boolean.TRUE.equals(b.getReminderSent()) && !now.isBefore(b.getLocksAt().minus(REMINDER_BEFORE))) {
                b.setReminderSent(true);
                Set<String> done = pickRepository.findByBracketId(b.getId()).stream().map(BracketPick::getUserId).collect(Collectors.toSet());
                for (User user : userRepository.findAll()) {
                    if (!done.contains(user.getId())) notify(user, "Plus que 24 h pour remplir le tableau « " + b.getLabel() + " ».", "Tableau à remplir");
                }
            }
            return;
        }
        readResults(b, teams);
        finishIfDone(b);
    }

    private static boolean sameGame(Match m, String a, String b) {
        String home = m.getHomeTeam().getAbbreviation();
        String away = m.getAwayTeam().getAbbreviation();
        return (home.equals(a) && away.equals(b)) || (home.equals(b) && away.equals(a));
    }

    // Une erreur de saisie dans la console ne doit pas laisser remplir son tableau match commencé.
    private void lockAtFirstRealGame(Bracket b, List<String> teams) {
        List<String> none = new ArrayList<>(Collections.nCopies(BracketTree.games(teams.size()), (String) null));
        Instant now = clock.instant();
        for (Match m : matchRepository.findByDateBetween(now.minus(Duration.ofDays(1)), b.getLocksAt())) {
            if (m.getType() != MatchType.NBA_CUP || m.getDate() == null || !m.getDate().isBefore(b.getLocksAt())) continue;
            for (int g = 0; g < teams.size() / 2; g++) {
                List<String> pair = BracketTree.participants(teams, none, g);
                if (sameGame(m, pair.get(0), pair.get(1))) {
                    b.setLocksAt(m.getDate());
                    break;
                }
            }
        }
    }

    private void readResults(Bracket b, List<String> teams) {
        List<String> winners = BracketTree.parse(b.getWinners());
        // Pas d'une éventuelle rencontre de poule entre les deux : seulement après le verrou.
        Instant from = b.getLocksAt().minus(Duration.ofHours(1));
        Map<String, Team> byAbbreviation = teamRepository.findAll().stream().collect(Collectors.toMap(Team::getAbbreviation, Function.identity(), (x, y) -> x));
        boolean changed = false;
        for (int g = 0; g < winners.size(); g++) {
            if (winners.get(g) != null) continue;
            List<String> pair = BracketTree.participants(teams, winners, g);
            if (pair.get(0) == null || pair.get(1) == null) continue;
            Team a = byAbbreviation.get(pair.get(0));
            Team c = byAbbreviation.get(pair.get(1));
            if (a == null || c == null) continue;
            for (Match m : matchRepository.findFinishedMeetings(a, c)) {
                if (m.getType() != MatchType.NBA_CUP || m.getDate().isBefore(from)) continue;
                winners = BracketTree.withWinner(teams, winners, g, m.getHomeScore() > m.getAwayScore() ? m.getHomeTeam().getAbbreviation() : m.getAwayTeam().getAbbreviation());
                changed = true;
                break;
            }
        }
        if (changed) b.setWinners(BracketTree.format(winners));
    }

    private void finishIfDone(Bracket b) {
        if (Boolean.TRUE.equals(b.getAwarded())) return;
        List<String> teams = BracketTree.parse(b.getTeams());
        List<String> winners = BracketTree.parse(b.getWinners());
        if (winners.isEmpty() || winners.get(winners.size() - 1) == null) return;
        b.setAwarded(true);
        Map<String, Integer> scores = pickRepository.findByBracketId(b.getId()).stream()
                .collect(Collectors.toMap(BracketPick::getUserId, p -> BracketTree.score(teams, winners, BracketTree.parse(p.getPicks()))));
        int best = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (best <= 0) return;
        scores.forEach((userId, points) -> {
            if (points != best) return;
            PhaseTitle title = new PhaseTitle();
            title.setUserId(userId);
            title.setSeason(b.getLabel());
            title.setPhase(PHASE);
            title.setPoints(points);
            titleRepository.save(title);
            userRepository.findById(userId).ifPresent(u -> notify(u, "Meilleur tableau « " + b.getLabel() + " » avec " + points + " pts ! Le titre rejoint ton palmarès.", "Meilleur tableau"));
        });
    }
}
