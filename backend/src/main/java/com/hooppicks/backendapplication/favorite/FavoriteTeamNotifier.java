package com.hooppicks.backendapplication.favorite;

import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Prévient les supporters d'une équipe : elle joue dans l'heure, puis son
 * résultat final. Une seule fois par match et par type (drapeaux sur Match),
 * selon la préférence notifyFavoriteTeam. Appelé par
 * FavoriteTeamNotifierScheduler ; `now` en paramètre pour les tests.
 */
@Service
public class FavoriteTeamNotifier {

    static final Duration KICKOFF_WINDOW = Duration.ofHours(1);
    // Fenêtre de rattrapage des résultats : un match fini il y a plus d'un
    // jour n'est plus une nouvelle (et évite d'en envoyer des dizaines au
    // premier démarrage).
    static final Duration RESULT_LOOKBACK = Duration.ofHours(30);

    // Surnoms singuliers en français : « le Heat », pas « les Heat ».
    private static final Set<String> SINGULAR = Set.of("Heat", "Jazz", "Magic", "Thunder");

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;

    public FavoriteTeamNotifier(MatchRepository matchRepository, UserRepository userRepository,
                                NotificationRepository notificationRepository, PushService pushService) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
    }

    @Transactional
    public int notifyUpcomingKickoffs(Instant now) {
        int sent = 0;
        for (Match match : matchRepository.findByStatusAndDateBetween(MatchStatus.SCHEDULED, now, now.plus(KICKOFF_WINDOW))) {
            if (Boolean.TRUE.equals(match.getFavoriteKickoffNotified())) continue;
            for (Team[] pair : sides(match)) {
                Team team = pair[0];
                Team opponent = pair[1];
                String message = subject(team) + " " + (isSingular(team) ? "joue" : "jouent")
                        + " dans moins d'une heure contre " + object(opponent) + ".";
                sent += notifyFans(team, "Ton équipe joue bientôt", message, match);
            }
            match.setFavoriteKickoffNotified(true);
            matchRepository.save(match);
        }
        return sent;
    }

    @Transactional
    public int notifyResults(Instant now) {
        int sent = 0;
        for (Match match : matchRepository.findByStatusAndDateBetween(MatchStatus.FINISHED, now.minus(RESULT_LOOKBACK), now)) {
            if (Boolean.TRUE.equals(match.getFavoriteResultNotified())) continue;
            if (match.getHomeScore() == null || match.getAwayScore() == null) continue;
            for (Team[] pair : sides(match)) {
                Team team = pair[0];
                Team opponent = pair[1];
                boolean home = team == match.getHomeTeam();
                int scored = home ? match.getHomeScore() : match.getAwayScore();
                int conceded = home ? match.getAwayScore() : match.getHomeScore();
                boolean won = scored > conceded;
                String message = (won ? "Victoire " : "Défaite ") + of(team) + " contre " + object(opponent)
                        + " (" + scored + "-" + conceded + ").";
                sent += notifyFans(team, won ? "Victoire de ton équipe !" : "Défaite de ton équipe", message, match);
            }
            match.setFavoriteResultNotified(true);
            matchRepository.save(match);
        }
        return sent;
    }

    private static List<Team[]> sides(Match match) {
        return List.of(
                new Team[]{match.getHomeTeam(), match.getAwayTeam()},
                new Team[]{match.getAwayTeam(), match.getHomeTeam()});
    }

    private int notifyFans(Team team, String title, String message, Match match) {
        int sent = 0;
        for (User user : userRepository.findByFavoriteTeamEndingWith(" " + team.getName())) {
            if (!user.isNotifyFavoriteTeam()) continue;
            AppNotification notification = new AppNotification();
            notification.setUser(user);
            notification.setType(NotificationType.FAVORITE_TEAM);
            notification.setMessage(message);
            notificationRepository.save(notification);
            pushService.sendToUser(user.getId(), new PushService.PushMessage(title, message, "/matches/" + match.getId()));
            sent++;
        }
        return sent;
    }

    private static boolean isSingular(Team team) {
        return SINGULAR.contains(team.getName());
    }

    /** « Les Knicks » / « Le Heat ». */
    static String subject(Team team) {
        return (isSingular(team) ? "Le " : "Les ") + team.getName();
    }

    /** « les Knicks » / « le Heat ». */
    static String object(Team team) {
        return (isSingular(team) ? "le " : "les ") + team.getName();
    }

    /** « des Knicks » / « du Heat ». */
    static String of(Team team) {
        return (isSingular(team) ? "du " : "des ") + team.getName();
    }
}
