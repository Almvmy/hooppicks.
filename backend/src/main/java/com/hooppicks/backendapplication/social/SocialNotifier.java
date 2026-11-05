package com.hooppicks.backendapplication.social;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Prévient un joueur quand un autre interagit avec lui : réaction à son ticket
 * ou à son message, @pseudo dans un fil. Cloche et push, selon
 * notifyLeagueActivity (c'est de l'activité entre joueurs, comme les duels).
 */
@Service
public class SocialNotifier {

    // Colonne de la cloche : 255 caractères.
    private static final int MAX_MESSAGE = 240;
    // Garde-fou : un message qui cite tout le monde ne notifie pas tout le monde.
    static final int MAX_MENTIONS = 5;
    private static final Pattern MENTION = Pattern.compile("(?<![\\p{L}\\p{N}_])@([\\p{L}\\p{N}_.-]{2,30})");

    private final BetRepository betRepository;
    private final LeagueMessageRepository messageRepository;
    private final WeeklyTitleRepository titleRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ReactionNotifiedRepository notifiedRepository;
    private final PushService pushService;

    public SocialNotifier(BetRepository betRepository, LeagueMessageRepository messageRepository,
                          WeeklyTitleRepository titleRepository, UserRepository userRepository,
                          NotificationRepository notificationRepository, ReactionNotifiedRepository notifiedRepository,
                          PushService pushService) {
        this.betRepository = betRepository;
        this.messageRepository = messageRepository;
        this.titleRepository = titleRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notifiedRepository = notifiedRepository;
        this.pushService = pushService;
    }

    // Facultatif : les tests construisent ce service sans lui.
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NightMessageRepository nightRepository;

    private record Target(User owner, String description, String link) {}

    private Optional<Target> target(String type, String id) {
        return switch (type) {
            case "BET" -> betRepository.findById(id).map(b -> new Target(b.getUser(),
                    "à ton ticket : " + b.getSelections().stream().map(BetSelection::getLabel).collect(Collectors.joining(" + ")), "/bets"));
            case "MESSAGE" -> messageRepository.findById(id).map(m -> new Target(m.getUser(),
                    "à ton message : « " + m.getText() + " »", "/leagues/" + m.getLeagueId()));
            case "TITLE" -> titleRepository.findById(id)
                    .flatMap(t -> userRepository.findById(t.getUserId()).map(u -> new Target(u, "à ton titre de champion",
                            t.getLeagueId() == null ? "/profile" : "/leagues/" + t.getLeagueId())));
            case "NIGHT" -> nightRepository == null ? Optional.empty() : nightRepository.findById(id)
                    .filter(m -> m.getUser() != null)
                    .map(m -> new Target(m.getUser(), "à ton message : « " + m.getText() + " »",
                            m.getDebateDay() != null ? "/debat" : m.getMatchId() == null ? "/soiree" : "/matches/" + m.getMatchId() + "?onglet=fil"));
            // « X a rejoint la ligue » : pas de quoi notifier.
            default -> Optional.empty();
        };
    }

    /** Une seule fois par joueur et par cible, quoi qu'il fasse ensuite de sa réaction. */
    public void reacted(User reactor, String targetType, String targetId, String emoji) {
        Target t = target(targetType, targetId).orElse(null);
        if (t == null || t.owner() == null || t.owner().getId().equals(reactor.getId())) return;
        String id = reactor.getId() + ":" + targetType + ":" + targetId;
        if (notifiedRepository.existsById(id)) return;
        ReactionNotified marker = new ReactionNotified();
        marker.setId(id);
        notifiedRepository.save(marker);
        send(t.owner(), reactor.getUsername() + " a réagi " + emoji + " " + t.description(), t.link(), "Nouvelle réaction");
    }

    /**
     * @pseudo dans un message. `allowedIds` : seuls ces joueurs peuvent être
     * cités (les membres, pour un fil de ligue) ; null : tout le monde.
     */
    public void mentions(User author, String text, String link, Set<String> allowedIds) {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = MENTION.matcher(text == null ? "" : text);
        while (m.find() && names.size() < MAX_MENTIONS) names.add(stripEnd(m.group(1)));
        for (String name : names) {
            User cited = userRepository.findByUsernameIgnoreCase(name).orElse(null);
            if (cited == null || cited.getId().equals(author.getId())) continue;
            if (allowedIds != null && !allowedIds.contains(cited.getId())) continue;
            send(cited, author.getUsername() + " t'a cité : « " + text + " »", link, "On parle de toi");
        }
    }

    // « @Kader. » : le point final n'appartient pas au pseudo.
    private static String stripEnd(String name) {
        return name.replaceAll("[.-]+$", "");
    }

    private void send(User to, String rawMessage, String link, String pushTitle) {
        String message = rawMessage.length() <= MAX_MESSAGE ? rawMessage : rawMessage.substring(0, MAX_MESSAGE - 1) + "…";
        AppNotification n = new AppNotification();
        n.setUser(to);
        n.setType(NotificationType.SOCIAL);
        n.setMessage(message);
        n.setLink(link);
        notificationRepository.save(n);
        if (to.isNotifyLeagueActivity()) {
            pushService.sendToUser(to.getId(), new PushService.PushMessage(pushTitle, message, link));
        }
    }
}
