package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.MatchFollowRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Suivre un match : notifications (cloche + push) au coup d'envoi, à chaque
 * fin de quart-temps, en fin de match serrée et au résultat.
 *
 * <p>Le déroulé vient du direct ESPN ({@link LiveMatchService#board()}, déjà
 * en cache : pas d'appel en plus). ESPN muet : le coup d'envoi et le
 * résultat partent quand même d'après le statut de la synchro balldontlie,
 * seules les fins de quart-temps et la fin serrée manquent. Chaque envoi est
 * noté sur {@link MatchFollow}, jamais deux fois.
 */
@Service
public class MatchFollowService {

    /** Au-delà, le joueur suit déjà assez de matchs à la fois. */
    public static final int MAX_ACTIVE = 30;
    static final int CLOSE_MARGIN = 5;
    static final int CLOSE_SECONDS = 120;
    // Un match suivi qui ne démarre jamais (reporté) ne doit pas rester actif indéfiniment.
    static final Duration ABANDON_AFTER = Duration.ofHours(12);

    public static class FollowException extends RuntimeException {
        public FollowException(String message) {
            super(message);
        }
    }

    private final MatchFollowRepository followRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final LiveMatchService liveMatchService;

    public MatchFollowService(MatchFollowRepository followRepository, MatchRepository matchRepository,
                              UserRepository userRepository, NotificationRepository notificationRepository,
                              PushService pushService, LiveMatchService liveMatchService) {
        this.followRepository = followRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.liveMatchService = liveMatchService;
    }

    public List<String> followedMatchIds(String userId) {
        return followRepository.findByUserId(userId).stream().map(MatchFollow::getMatchId).toList();
    }

    @Transactional
    public void follow(String userId, String matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new FollowException("Match introuvable."));
        if (match.getStatus() == MatchStatus.FINISHED) throw new FollowException("Ce match est terminé.");
        if (followRepository.findByUserIdAndMatchId(userId, matchId).isPresent()) return;
        if (followRepository.countActiveForUser(userId) >= MAX_ACTIVE) {
            throw new FollowException("Tu suis déjà " + MAX_ACTIVE + " matchs : retire-en un d'abord.");
        }
        MatchFollow follow = new MatchFollow();
        follow.setUserId(userId);
        follow.setMatchId(matchId);
        // Suivi en cours de match : pas de « C'est parti ! » une minute après.
        if (match.getStatus() == MatchStatus.LIVE) follow.setKickoffNotified(true);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(String userId, String matchId) {
        followRepository.findByUserIdAndMatchId(userId, matchId).ifPresent(followRepository::delete);
    }

    /** Un passage : envoie ce qui est dû pour chaque match suivi. Renvoie le nombre de notifications. */
    @Transactional
    public int notifyFollowers(Instant now) {
        List<MatchFollow> active = followRepository.findActive();
        if (active.isEmpty()) return 0;
        Map<String, Match> matches = matchRepository.findAllById(
                        active.stream().map(MatchFollow::getMatchId).distinct().toList()).stream()
                .collect(Collectors.toMap(Match::getId, Function.identity()));
        Map<String, LiveMatchService.LiveStatusDto> live = liveMatchService.board().stream()
                .collect(Collectors.toMap(LiveMatchService.LiveStatusDto::matchId, Function.identity(), (a, b) -> a));

        int sent = 0;
        for (MatchFollow follow : active) {
            Match match = matches.get(follow.getMatchId());
            if (match == null) {
                followRepository.delete(follow);
                continue;
            }
            LiveMatchService.LiveStatusDto status = live.get(match.getId());
            sent += step(follow, match, status, now);
        }
        return sent;
    }

    private int step(MatchFollow f, Match match, LiveMatchService.LiveStatusDto live, Instant now) {
        boolean espnStarted = live != null && !"pre".equals(live.state());
        boolean finished = match.getStatus() == MatchStatus.FINISHED || (live != null && "post".equals(live.state()));
        int sent = 0;

        if (match.getDate() != null && match.getStatus() == MatchStatus.SCHEDULED && !espnStarted
                && now.isAfter(match.getDate().plus(ABANDON_AFTER))) {
            f.setFinalNotified(true);
            return 0;
        }

        if (!Boolean.TRUE.equals(f.getKickoffNotified()) && (espnStarted || match.getStatus() == MatchStatus.LIVE) && !finished) {
            send(f, match, "C'est parti !", match.getAwayTeam().getName() + " - " + match.getHomeTeam().getName() + " a commencé.");
            f.setKickoffNotified(true);
            sent++;
        }

        if (live != null && "in".equals(live.state())) {
            int last = f.getLastPeriodNotified() == null ? 0 : f.getLastPeriodNotified();
            if (periodEnded(live) && live.period() > last) {
                send(f, match, live.period() == 2 ? "Mi-temps" : "Fin " + periodName(live.period()), score(match, live));
                f.setLastPeriodNotified(live.period());
                sent++;
            }
            if (!Boolean.TRUE.equals(f.getCloseFinishNotified()) && isCloseFinish(live)) {
                send(f, match, "Fin de match serrée", score(match, live) + ", " + live.clock() + " à jouer.");
                f.setCloseFinishNotified(true);
                sent++;
            }
        }

        if (finished) {
            send(f, match, "Terminé", score(match, live));
            f.setFinalNotified(true);
            sent++;
        }
        return sent;
    }

    /** Chrono à zéro (ESPN écrit « 0.0 ») ou mi-temps : la période vient de finir. */
    static boolean periodEnded(LiveMatchService.LiveStatusDto live) {
        String detail = live.detail() == null ? "" : live.detail().toLowerCase(Locale.ROOT);
        return "0.0".equals(live.clock()) || "0:00".equals(live.clock()) || detail.contains("halftime") || detail.startsWith("end of");
    }

    static boolean isCloseFinish(LiveMatchService.LiveStatusDto live) {
        if (live.period() < 4 || live.homeScore() == null || live.awayScore() == null) return false;
        int seconds = clockSeconds(live.clock());
        return seconds > 0 && seconds <= CLOSE_SECONDS && Math.abs(live.homeScore() - live.awayScore()) <= CLOSE_MARGIN;
    }

    static int clockSeconds(String clock) {
        if (clock == null) return -1;
        try {
            String[] parts = clock.split(":");
            return parts.length == 2
                    ? Integer.parseInt(parts[0]) * 60 + (int) Double.parseDouble(parts[1])
                    : (int) Double.parseDouble(clock);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String periodName(int period) {
        if (period <= 4) return "du " + (period == 1 ? "1er" : period + "e") + " quart-temps";
        return period == 5 ? "de la prolongation" : "de la " + (period - 4) + "e prolongation";
    }

    /** « HOU 65 - 45 DAL » : score du direct s'il y en a un, sinon celui de la synchro. */
    private static String score(Match match, LiveMatchService.LiveStatusDto live) {
        Integer away = live != null && live.awayScore() != null ? live.awayScore() : match.getAwayScore();
        Integer home = live != null && live.homeScore() != null ? live.homeScore() : match.getHomeScore();
        return match.getAwayTeam().getAbbreviation() + " " + (away == null ? 0 : away) + " - "
                + (home == null ? 0 : home) + " " + match.getHomeTeam().getAbbreviation();
    }

    private void send(MatchFollow f, Match match, String title, String message) {
        User user = userRepository.findById(f.getUserId()).orElse(null);
        if (user == null) return;
        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setType(NotificationType.MATCH_FOLLOW);
        notification.setMessage(title + " · " + message);
        notificationRepository.save(notification);
        pushService.sendToUser(user.getId(), new PushService.PushMessage(title, message, "/matches/" + match.getId()));
    }
}
