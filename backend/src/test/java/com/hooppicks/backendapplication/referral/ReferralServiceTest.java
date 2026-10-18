package com.hooppicks.backendapplication.referral;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReferralServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final PushService pushService = mock(PushService.class);
    private final ReferralService service = new ReferralService(userRepository, notificationRepository, pushService,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private User user(String id, String username, Instant createdAt) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setCreatedAt(createdAt);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.findByUsernameIgnoreCase(username.toLowerCase())).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void nouveau_compte_rattache_a_son_parrain_qui_est_prevenu() {
        User parrain = user("p", "Parrain", NOW.minusSeconds(86_400 * 30));
        User recrue = user("r", "Recrue", NOW.minusSeconds(60));
        when(userRepository.countByReferredBy("p")).thenReturn(3L);

        assertThat(service.claim("r", "parrain")).isTrue();

        assertThat(recrue.getReferredBy()).isEqualTo("p");
        verify(pushService).sendToUser(eq("p"), argThat(m -> m.body().contains("@Recrue") && m.body().contains("Recruteur débloqué")));
        assertThat(parrain.getReferredBy()).isNull();
    }

    @Test
    void ni_compte_ancien_ni_soi_meme_ni_deux_fois() {
        user("vieux", "Vieux", NOW.minusSeconds(7200));
        user("p", "Parrain", NOW.minusSeconds(86_400));
        User deja = user("d", "Deja", NOW.minusSeconds(60));
        deja.setReferredBy("x");
        user("moi", "Moi", NOW.minusSeconds(60));

        assertThat(service.claim("vieux", "parrain")).isFalse();
        assertThat(service.claim("d", "parrain")).isFalse();
        assertThat(service.claim("moi", "moi")).isFalse();
        assertThat(service.claim("moi", "inconnu")).isFalse();
        verify(userRepository, never()).save(any());
        verifyNoInteractions(pushService);
    }
}
