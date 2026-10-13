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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PushService pushService;
    @Mock private AdminAuditService auditService;

    private FeedbackService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(feedbackRepository, userRepository, notificationRepository, pushService, auditService);
    }

    private static User user(String id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    @Test
    void enregistre_le_message_avec_la_page_et_l_appareil() {
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackDto dto = service.submit("u1",
                new FeedbackService.SubmitRequest("bug", "  Le direct ne bouge plus  ", "/matches/42", true), "Mozilla/5.0 iPhone");

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertThat(saved.getValue().getCategory()).isEqualTo("BUG");
        assertThat(saved.getValue().getMessage()).isEqualTo("Le direct ne bouge plus");
        assertThat(saved.getValue().getPage()).isEqualTo("/matches/42");
        assertThat(saved.getValue().getDevice()).isEqualTo("Mozilla/5.0 iPhone");
        assertThat(saved.getValue().getStatus()).isEqualTo("OPEN");
        // Le joueur ne reçoit jamais en retour son appareil ni son pseudo : réservé à la console.
        assertThat(dto.device()).isNull();
    }

    @Test
    void refuse_un_type_inconnu_ou_un_message_vide() {
        assertThatThrownBy(() -> service.submit("u1", new FeedbackService.SubmitRequest("SPAM", "Bonjour à tous", null, null), null))
                .isInstanceOf(AdminActionException.class);
        assertThatThrownBy(() -> service.submit("u1", new FeedbackService.SubmitRequest("IDEA", "   ", null, null), null))
                .isInstanceOf(AdminActionException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void limite_a_cinq_messages_par_jour() {
        when(feedbackRepository.countByUserIdAndCreatedAtAfter(eq("u1"), any())).thenReturn(5L);

        assertThatThrownBy(() -> service.submit("u1", new FeedbackService.SubmitRequest("IDEA", "Encore une idée", null, null), null))
                .isInstanceOf(AdminActionException.class)
                .extracting(e -> ((AdminActionException) e).getStatus()).isEqualTo(429);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void la_reponse_part_dans_la_cloche_en_push_et_au_journal() {
        Feedback feedback = new Feedback();
        feedback.setId("f1");
        feedback.setUserId("u1");
        when(feedbackRepository.findById("f1")).thenReturn(Optional.of(feedback));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "kobe")));

        FeedbackDto dto = service.reply("f1", "Corrigé, merci !", user("a1", "admin"));

        assertThat(dto.status()).isEqualTo("ANSWERED");
        assertThat(feedback.getReply()).isEqualTo("Corrigé, merci !");
        assertThat(feedback.getRepliedBy()).isEqualTo("admin");

        ArgumentCaptor<AppNotification> notif = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notif.capture());
        assertThat(notif.getValue().getType()).isEqualTo(NotificationType.FEEDBACK_REPLY);
        assertThat(notif.getValue().getMessage()).contains("Corrigé, merci !");
        verify(pushService).sendToUser(eq("u1"), any());
        verify(auditService).log(any(), eq("FEEDBACK_REPLY"), eq("kobe"), anyString());
    }

    @Test
    void une_longue_reponse_tient_dans_la_notification() {
        Feedback feedback = new Feedback();
        feedback.setUserId("u1");
        when(feedbackRepository.findById("f1")).thenReturn(Optional.of(feedback));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "kobe")));

        service.reply("f1", "x".repeat(900), user("a1", "admin"));

        ArgumentCaptor<AppNotification> notif = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(notif.capture());
        assertThat(notif.getValue().getMessage()).hasSizeLessThanOrEqualTo(255).endsWith("…");
        assertThat(feedback.getReply()).hasSize(900);
    }

    @Test
    void rouvrir_un_message_repondu_le_remet_en_repondu() {
        Feedback feedback = new Feedback();
        feedback.setUserId("u1");
        feedback.setReply("Déjà répondu");
        feedback.setStatus("CLOSED");
        when(feedbackRepository.findById("f1")).thenReturn(Optional.of(feedback));

        service.setClosed("f1", false, user("a1", "admin"));

        assertThat(feedback.getStatus()).isEqualTo("ANSWERED");
    }
}
