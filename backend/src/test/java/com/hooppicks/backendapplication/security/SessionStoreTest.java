package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.entity.UserSession;
import com.hooppicks.backendapplication.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionStoreTest {

    @Mock private UserSessionRepository repository;

    private SessionStore store;
    /** La « base » : empreinte du jeton -> session. */
    private final Map<String, UserSession> db = new HashMap<>();

    @BeforeEach
    void setUp() {
        store = new SessionStore(repository);
        when(repository.save(any(UserSession.class))).thenAnswer(inv -> {
            UserSession s = inv.getArgument(0);
            db.put(s.getTokenHash(), s);
            return s;
        });
        when(repository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(db.get(inv.<String>getArgument(0))));
        doAnswer(inv -> db.remove(inv.<String>getArgument(0))).when(repository).deleteById(anyString());
        when(repository.deleteAllForUser(anyString())).thenAnswer(inv -> {
            String userId = inv.getArgument(0);
            db.values().removeIf(s -> s.getUserId().equals(userId));
            return 1;
        });
    }

    @Test
    void la_base_ne_garde_que_l_empreinte_du_jeton_et_la_session_dure_60_jours() {
        String token = store.createSession("u1");

        ArgumentCaptor<UserSession> saved = ArgumentCaptor.forClass(UserSession.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(token).hasSize(64);
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(59)));
        assertThat(store.getUserId(token)).isEqualTo("u1");
    }

    @Test
    void une_session_survit_a_un_redemarrage_du_serveur() {
        String token = store.createSession("u1");

        // Nouveau SessionStore = serveur redémarré : cache mémoire vide, même base.
        SessionStore apresRedeploiement = new SessionStore(repository);
        assertThat(apresRedeploiement.getUserId(token)).isEqualTo("u1");
    }

    @Test
    void jeton_inconnu_ou_session_expiree_refuses() {
        assertThat(store.getUserId("inconnu")).isNull();

        String token = store.createSession("u1");
        db.get(SessionStore.hash(token)).setExpiresAt(Instant.now().minusSeconds(1));
        assertThat(new SessionStore(repository).getUserId(token)).isNull();
    }

    @Test
    void la_deconnexion_et_le_changement_de_mot_de_passe_ferment_aussitot_les_sessions() {
        String a = store.createSession("u1");
        String b = store.createSession("u1");
        String autre = store.createSession("u2");
        assertThat(store.getUserId(a)).isEqualTo("u1"); // mis en cache

        store.invalidate(a);
        assertThat(store.getUserId(a)).isNull();

        store.invalidateAllForUser("u1");
        assertThat(store.getUserId(b)).isNull();
        assertThat(store.getUserId(autre)).isEqualTo("u2");
    }

    @Test
    void prolongee_une_fois_par_jour_au_plus() {
        String token = store.createSession("u1");
        assertThat(store.renewIfDue(token)).isFalse(); // créée à l'instant

        UserSession s = db.get(SessionStore.hash(token));
        s.setLastSeenAt(Instant.now().minus(Duration.ofDays(2)));
        s.setExpiresAt(Instant.now().plus(Duration.ofDays(3)));
        SessionStore fresh = new SessionStore(repository);

        assertThat(fresh.renewIfDue(token)).isTrue();
        assertThat(s.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(59)));
        assertThat(fresh.renewIfDue(token)).isFalse();
    }
}
