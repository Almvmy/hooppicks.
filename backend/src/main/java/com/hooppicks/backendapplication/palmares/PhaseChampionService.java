package com.hooppicks.backendapplication.palmares;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * Classements Coupe NBA et playoffs : ouverts une fois le premier match de
 * la phase commencé, puis titre de champion (tout HoopPicks et chaque ligue
 * d'au moins 2 joueurs classés, ex æquo compris) quand la phase est finie :
 * finale de la Coupe terminée, ou match des Finales NBA qui clôt la série.
 * Attendu que les tickets de la phase soient réglés (24 h au plus après le
 * dernier match), comme pour le champion de la semaine. Une fois par saison
 * et par phase (`PalmaresPhase`).
 */
@Service
public class PhaseChampionService {

    private static final Logger log = LoggerFactory.getLogger(PhaseChampionService.class);
    private static final Duration GRACE = Duration.ofHours(24);
    private static final List<LeaderboardPeriod> PHASES = List.of(LeaderboardPeriod.CUP, LeaderboardPeriod.PLAYOFFS);

    private final BetRepository betRepository;
    private final MatchRepository matchRepository;
    private final PhaseTitleRepository titleRepository;
    private final PalmaresPhaseRepository phaseRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final Clock clock;

    @Autowired
    public PhaseChampionService(BetRepository betRepository, MatchRepository matchRepository,
                                PhaseTitleRepository titleRepository, PalmaresPhaseRepository phaseRepository,
                                LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                                UserRepository userRepository, NotificationRepository notificationRepository,
                                PushService pushService) {
        this(betRepository, matchRepository, titleRepository, phaseRepository, leagueRepository, membershipRepository,
                userRepository, notificationRepository, pushService, Clock.system(BankrollService.ZONE));
    }

    PhaseChampionService(BetRepository betRepository, MatchRepository matchRepository,
                         PhaseTitleRepository titleRepository, PalmaresPhaseRepository phaseRepository,
                         LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                         UserRepository userRepository, NotificationRepository notificationRepository,
                         PushService pushService, Clock clock) {
        this.betRepository = betRepository;
        this.matchRepository = matchRepository;
        this.titleRepository = titleRepository;
        this.phaseRepository = phaseRepository;
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.clock = clock;
    }

    public record OpenPhases(boolean cup, boolean playoffs) {}

    /** Phases dont le classement s'affiche : premier match commencé cette saison. */
    @Transactional(readOnly = true)
    public OpenPhases openPhases() {
        Instant now = clock.instant();
        Instant from = seasonStart();
        return new OpenPhases(
                matchRepository.existsByTypeInAndDateBetween(types(LeaderboardPeriod.CUP), from, now),
                matchRepository.existsByTypeInAndDateBetween(types(LeaderboardPeriod.PLAYOFFS), from, now));
    }

    /** Attribue les titres des phases terminées. Renvoie le nombre de phases traitées. */
    @Transactional
    public int recordEndedPhases() {
        Instant now = clock.instant();
        Instant from = seasonStart();
        String season = season();
        int done = 0;
        for (LeaderboardPeriod phase : PHASES) {
            String id = season + ":" + phase.name();
            if (phaseRepository.existsById(id)) continue;
            Optional<Instant> end = phaseEnd(phase, from);
            if (end.isEmpty()) continue;
            if (betRepository.countPendingInPhaseSince(from, phase.phase()) > 0 && now.isBefore(end.get().plus(GRACE))) continue;
            award(season, phase, from);
            PalmaresPhase marker = new PalmaresPhase();
            marker.setId(id);
            marker.setAwardedAt(now);
            phaseRepository.save(marker);
            done++;
        }
        return done;
    }

    // Coup d'envoi du match qui clôt la phase, une fois terminé. La finale de
    // la Coupe est un match sec ; les Finales NBA se jouent en série, finie
    // quand ESPN annonce « remporte la série ».
    private Optional<Instant> phaseEnd(LeaderboardPeriod phase, Instant from) {
        if (phase == LeaderboardPeriod.CUP) {
            return matchRepository.findByTypeAndStatusAndDateAfter(MatchType.NBA_CUP, MatchStatus.FINISHED, from).stream()
                    .filter(m -> "Coupe NBA · Finale".equals(m.getStageLabel()))
                    .map(Match::getDate).max(Comparator.naturalOrder());
        }
        return matchRepository.findByTypeAndStatusAndDateAfter(MatchType.PLAYOFFS, MatchStatus.FINISHED, from).stream()
                .filter(m -> m.getStageLabel() != null && m.getStageLabel().startsWith("Finales NBA"))
                .filter(m -> m.getSeriesSummary() != null && m.getSeriesSummary().contains("remporte la série"))
                .map(Match::getDate).max(Comparator.naturalOrder());
    }

    private void award(String season, LeaderboardPeriod phase, Instant from) {
        Map<String, Long> scores = new HashMap<>();
        for (Object[] row : betRepository.getLeaderboardRawSince(from, phase.phase())) {
            scores.put((String) row[0], ((Number) row[2]).longValue());
        }
        int titles = grant(season, phase, scores, null, null);
        for (League league : leagueRepository.findAll()) {
            Map<String, Long> members = new HashMap<>();
            for (LeagueMembership m : membershipRepository.findByLeagueId(league.getId())) {
                Long s = scores.get(m.getUser().getId());
                if (s != null) members.put(m.getUser().getId(), s);
            }
            if (members.size() >= 2) titles += grant(season, phase, members, league.getId(), league.getName());
        }
        log.info("Palmarès : {} {} attribué ({} titre(s))", phase.name(), season, titles);
    }

    private int grant(String season, LeaderboardPeriod phase, Map<String, Long> scores, String leagueId, String leagueName) {
        long best = scores.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (best <= 0) return 0;
        int count = 0;
        for (Map.Entry<String, Long> e : scores.entrySet()) {
            if (e.getValue() != best) continue;
            PhaseTitle t = new PhaseTitle();
            t.setUserId(e.getKey());
            t.setSeason(season);
            t.setPhase(phase.name());
            t.setLeagueId(leagueId);
            t.setLeagueName(leagueName);
            t.setPoints(best);
            titleRepository.save(t);
            count++;
            notifyChampion(e.getKey(), phase, leagueName, best);
        }
        return count;
    }

    private void notifyChampion(String userId, LeaderboardPeriod phase, String leagueName, long points) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        String title = title(phase);
        String message = leagueName == null
                ? title + " sur tout HoopPicks avec " + points + " pts ! Le titre rejoint ton palmarès."
                : title + " dans ta ligue « " + leagueName + " » avec " + points + " pts !";
        AppNotification n = new AppNotification();
        n.setUser(user);
        n.setType(NotificationType.SYSTEM);
        n.setMessage(message);
        notificationRepository.save(n);
        if (user.isNotifyLeagueActivity()) {
            pushService.sendToUser(userId, new PushService.PushMessage(title, message, "/profile"));
        }
    }

    static String title(LeaderboardPeriod phase) {
        return phase == LeaderboardPeriod.CUP ? "Champion de la Coupe NBA" : "Champion des playoffs";
    }

    /** Liste vide tant que la phase n'est pas finie. */
    public record PhaseChampions(String season, String phase, List<PalmaresService.Champion> champions) {}

    @Transactional(readOnly = true)
    public PhaseChampions champions(LeaderboardPeriod phase, String leagueId) {
        String season = season();
        List<PhaseTitle> titles = leagueId == null
                ? titleRepository.findBySeasonAndPhaseAndLeagueIdIsNull(season, phase.name())
                : titleRepository.findBySeasonAndPhaseAndLeagueId(season, phase.name(), leagueId);
        Map<String, Long> points = new HashMap<>();
        titles.forEach(t -> points.put(t.getUserId(), t.getPoints()));
        List<PalmaresService.Champion> champions = userRepository.findAllById(points.keySet()).stream()
                .map(u -> new PalmaresService.Champion(u.getUsername(), u.getAvatarNumber(), u.getAvatarPosition(),
                        u.getAvatarColorway(), u.getAvatarIcon(), u.getFavoriteTeam(), points.get(u.getId())))
                .sorted(Comparator.comparing(PalmaresService.Champion::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new PhaseChampions(season, phase.name().toLowerCase(), champions);
    }

    private static Collection<MatchType> types(LeaderboardPeriod phase) {
        return phase == LeaderboardPeriod.CUP ? List.of(MatchType.NBA_CUP) : List.of(MatchType.PLAY_IN, MatchType.PLAYOFFS);
    }

    private Instant seasonStart() {
        return LeaderboardPeriod.seasonStart(ZonedDateTime.now(clock));
    }

    private String season() {
        int start = seasonStart().atZone(BankrollService.ZONE).getYear();
        return start + "-" + String.valueOf(start + 1).substring(2);
    }
}
