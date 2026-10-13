package com.hooppicks.backendapplication.feedback;

import com.hooppicks.backendapplication.admin.AdminActionException;
import com.hooppicks.backendapplication.admin.AdminAuditService;
import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Feedback;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.FeedbackRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Messages des joueurs à l'équipe et réponses de la console admin. La
 * réponse arrive dans la cloche (et en push) : le joueur est toujours
 * prévenu, quelles que soient ses préférences, puisque c'est lui qui a écrit.
 */
@Service
public class FeedbackService {

    static final Set<String> CATEGORIES = Set.of("BUG", "QUESTION", "IDEA", "OTHER");
    static final int MAX_LENGTH = 1000;
    static final int MAX_PER_DAY = 5;
    /** Le message d'une notification tient dans une colonne de 255 caractères. */
    static final int NOTIFICATION_MAX = 255;

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final AdminAuditService auditService;

    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository,
                           NotificationRepository notificationRepository, PushService pushService,
                           AdminAuditService auditService) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.auditService = auditService;
    }

    public record SubmitRequest(String category, String message, String page, Boolean appInstalled) {}

    @Transactional
    public FeedbackDto submit(String userId, SubmitRequest request, String userAgent) {
        String category = request.category() == null ? "" : request.category().trim().toUpperCase();
        if (!CATEGORIES.contains(category)) throw new AdminActionException(400, "Choisis le type de message.");
        String message = request.message() == null ? "" : request.message().trim();
        if (message.length() < 5) throw new AdminActionException(400, "Ton message est un peu court.");
        if (message.length() > MAX_LENGTH) {
            throw new AdminActionException(400, "Message trop long (" + MAX_LENGTH + " caractères au plus).");
        }
        if (feedbackRepository.countByUserIdAndCreatedAtAfter(userId, Instant.now().minus(Duration.ofDays(1))) >= MAX_PER_DAY) {
            throw new AdminActionException(429, "Tu as déjà envoyé " + MAX_PER_DAY + " messages aujourd'hui. On lit tout, promis : réessaie demain.");
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setCategory(category);
        feedback.setMessage(message);
        feedback.setPage(truncate(request.page(), 255));
        feedback.setDevice(truncate(userAgent, 400));
        feedback.setAppInstalled(request.appInstalled());
        return FeedbackDto.forPlayer(feedbackRepository.save(feedback));
    }

    public List<FeedbackDto> mine(String userId) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(FeedbackDto::forPlayer).toList();
    }

    public List<FeedbackDto> forConsole(String status) {
        List<Feedback> list = status == null || status.isBlank()
                ? feedbackRepository.findTop200ByOrderByCreatedAtDesc()
                : feedbackRepository.findTop200ByStatusOrderByCreatedAtDesc(status.toUpperCase());
        Map<String, String> usernames = userRepository.findAllById(list.stream().map(Feedback::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        return list.stream().map(f -> FeedbackDto.forAdmin(f, usernames.get(f.getUserId()))).toList();
    }

    public long openCount() {
        return feedbackRepository.countByStatus("OPEN");
    }

    @Transactional
    public FeedbackDto reply(String feedbackId, String text, User admin) {
        String reply = text == null ? "" : text.trim();
        if (reply.isEmpty() || reply.length() > MAX_LENGTH) {
            throw new AdminActionException(400, "La réponse doit faire entre 1 et " + MAX_LENGTH + " caractères.");
        }
        Feedback feedback = find(feedbackId);
        User player = userRepository.findById(feedback.getUserId())
                .orElseThrow(() -> new AdminActionException(404, "Le joueur a supprimé son compte."));

        feedback.setReply(reply);
        feedback.setRepliedAt(Instant.now());
        feedback.setRepliedBy(admin.getUsername());
        feedback.setStatus("ANSWERED");
        feedbackRepository.save(feedback);

        // La cloche n'affiche qu'un extrait : la réponse entière est sur la page Nous écrire (/contact).
        AppNotification n = new AppNotification();
        n.setUser(player);
        n.setType(NotificationType.FEEDBACK_REPLY);
        n.setMessage(truncate("L'équipe HoopPicks t'a répondu : « " + reply + " »", NOTIFICATION_MAX));
        notificationRepository.save(n);
        pushService.sendToUser(player.getId(),
                new PushService.PushMessage("L'équipe HoopPicks t'a répondu", truncate(reply, 180), "/contact"));

        auditService.log(admin, "FEEDBACK_REPLY", player.getUsername(), truncate(reply, 200));
        return FeedbackDto.forAdmin(feedback, player.getUsername());
    }

    /** Traité sans réponse (doublon, message sans objet…), ou rouvert. */
    @Transactional
    public FeedbackDto setClosed(String feedbackId, boolean closed, User admin) {
        Feedback feedback = find(feedbackId);
        feedback.setStatus(closed ? "CLOSED" : (feedback.getReply() != null ? "ANSWERED" : "OPEN"));
        feedbackRepository.save(feedback);
        String username = userRepository.findById(feedback.getUserId()).map(User::getUsername).orElse(null);
        auditService.log(admin, closed ? "FEEDBACK_CLOSE" : "FEEDBACK_REOPEN", username, null);
        return FeedbackDto.forAdmin(feedback, username);
    }

    private Feedback find(String id) {
        return feedbackRepository.findById(id).orElseThrow(() -> new AdminActionException(404, "Message introuvable."));
    }

    static String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
