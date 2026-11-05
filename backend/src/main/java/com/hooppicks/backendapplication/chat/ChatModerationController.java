package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.admin.AdminAuditService;
import com.hooppicks.backendapplication.bet.PlayerPropsService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.*;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Modération du fil de la soirée, onglet « Signalements » de la console : même
 * accès que /console (session + User.isAdmin), chaque décision au journal d'audit.
 */
@RestController
@RequestMapping("/console/chat")
public class ChatModerationController {

    public record ReportedMessage(String id, String username, String text, String matchId, Instant createdAt, long reports, boolean hidden) {}
    public record MutedUser(String username, Instant until) {}
    public record Overview(List<ReportedMessage> reported, List<String> words, List<MutedUser> muted) {}
    public record MuteRequest(Integer days) {}
    public record WordRequest(String word) {}

    private final SessionStore sessionStore;
    private final UserRepository userRepository;
    private final NightMessageRepository messageRepository;
    private final MessageReportRepository reportRepository;
    private final ChatWordRepository wordRepository;
    private final NightThreadService threadService;
    private final AdminAuditService auditService;

    public ChatModerationController(SessionStore sessionStore, UserRepository userRepository, NightMessageRepository messageRepository,
                                    MessageReportRepository reportRepository, ChatWordRepository wordRepository,
                                    NightThreadService threadService, AdminAuditService auditService) {
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.reportRepository = reportRepository;
        this.wordRepository = wordRepository;
        this.threadService = threadService;
        this.auditService = auditService;
    }

    private User admin(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return null;
        return userRepository.findById(userId).filter(User::isAdmin).orElse(null);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> overview(HttpServletRequest request) {
        if (admin(request) == null) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(new Overview(reported(),
                wordRepository.findAll().stream().map(ChatWord::getWord).sorted().toList(),
                userRepository.findByChatMutedUntilAfter(Instant.now()).stream()
                        .map(u -> new MutedUser(u.getUsername(), u.getChatMutedUntil())).toList()));
    }

    private List<ReportedMessage> reported() {
        List<String> ids = reportRepository.findReportedMessageIds();
        if (ids.isEmpty()) return List.of();
        Map<String, Long> counts = reportRepository.findByMessageIdIn(ids).stream()
                .collect(Collectors.groupingBy(MessageReport::getMessageId, Collectors.counting()));
        return messageRepository.findAllById(ids).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getReviewed()))
                .map(m -> new ReportedMessage(m.getId(), m.getUser() == null ? null : m.getUser().getUsername(), m.getText(), m.getMatchId(),
                        m.getCreatedAt(), counts.getOrDefault(m.getId(), 0L), Boolean.TRUE.equals(m.getHidden())))
                .sorted(Comparator.comparing(ReportedMessage::hidden).reversed().thenComparing(ReportedMessage::createdAt, Comparator.reverseOrder()))
                .toList();
    }

    @PostMapping("/messages/{id}/delete")
    @Transactional
    public ResponseEntity<?> deleteMessage(@PathVariable String id, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        NightMessage m = messageRepository.findById(id).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();
        auditService.log(admin, "Message du fil supprimé", m.getUser() == null ? id : m.getUser().getUsername(), m.getText());
        threadService.remove(m);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    /** Signalement écarté : le message réapparaît et ne pourra plus être masqué par des signalements. */
    @PostMapping("/messages/{id}/restore")
    @Transactional
    public ResponseEntity<?> restoreMessage(@PathVariable String id, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        NightMessage m = messageRepository.findById(id).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();
        m.setHidden(false);
        m.setReviewed(true);
        reportRepository.deleteAllForMessage(id);
        auditService.log(admin, "Message du fil rétabli", m.getUser() == null ? id : m.getUser().getUsername(), m.getText());
        return ResponseEntity.ok(Map.of("restored", true));
    }

    @PostMapping("/users/{username}/mute")
    @Transactional
    public ResponseEntity<?> mute(@PathVariable String username, @RequestBody MuteRequest body, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        int days = body.days() == null ? 7 : Math.max(1, Math.min(365, body.days()));
        user.setChatMutedUntil(Instant.now().plus(Duration.ofDays(days)));
        auditService.log(admin, "Parole coupée dans le fil", username, days + " jour(s)");
        return ResponseEntity.ok(Map.of("days", days));
    }

    @PostMapping("/users/{username}/unmute")
    @Transactional
    public ResponseEntity<?> unmute(@PathVariable String username, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setChatMutedUntil(null);
        auditService.log(admin, "Parole rendue dans le fil", username, null);
        return ResponseEntity.ok(Map.of("unmuted", true));
    }

    @PostMapping("/words")
    public ResponseEntity<?> addWord(@RequestBody WordRequest body, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        String word = PlayerPropsService.normalize(body.word());
        if (word.isBlank()) return ResponseEntity.badRequest().body("Mot vide.");
        ChatWord entry = new ChatWord();
        entry.setWord(word);
        wordRepository.save(entry);
        auditService.log(admin, "Mot refusé ajouté au fil", word, null);
        return ResponseEntity.ok(Map.of("word", word));
    }

    @PostMapping("/words/delete")
    public ResponseEntity<?> removeWord(@RequestBody WordRequest body, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        wordRepository.deleteById(PlayerPropsService.normalize(body.word()));
        auditService.log(admin, "Mot refusé retiré du fil", body.word(), null);
        return ResponseEntity.ok(Map.of("removed", true));
    }
}
