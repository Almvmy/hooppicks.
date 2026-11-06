package com.hooppicks.backendapplication.social;

import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MentionControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final LeagueMembershipRepository memberships = mock(LeagueMembershipRepository.class);
    private final SessionStore sessions = mock(SessionStore.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final MentionController controller = new MentionController(users, memberships, sessions);

    private static User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        return u;
    }

    private static LeagueMembership member(User u) {
        LeagueMembership m = new LeagueMembership();
        m.setUser(u);
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<String> names(ResponseEntity<?> response) {
        return ((List<MentionController.Candidate>) response.getBody()).stream().map(MentionController.Candidate::username).toList();
    }

    @BeforeEach
    void setUp() {
        when(sessions.getUserIdFromRequest(any())).thenReturn("me");
    }

    @Test
    void sans_compte_rien() {
        when(sessions.getUserIdFromRequest(any())).thenReturn(null);
        assertThat(controller.mentions("ka", null, request).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void propose_les_pseudos_par_debut_sans_soi_meme() {
        when(users.findTop7ByUsernameStartingWithIgnoreCaseOrderByUsernameAsc("ka")).thenReturn(List.of(user("me", "Kamo"), user("k", "Kader")));

        assertThat(names(controller.mentions("ka", null, request))).containsExactly("Kader");
    }

    @Test
    void sans_premiere_lettre_on_ne_liste_pas_toute_la_base() {
        assertThat(names(controller.mentions("", null, request))).isEmpty();
        verify(users, never()).findTop7ByUsernameStartingWithIgnoreCaseOrderByUsernameAsc(any());
    }

    @Test
    void dans_une_ligue_seulement_ses_membres() {
        when(memberships.findByLeagueIdAndUserId("l1", "me")).thenReturn(Optional.of(new LeagueMembership()));
        when(memberships.findByLeagueId("l1")).thenReturn(List.of(member(user("me", "Kamo")), member(user("k", "Kader")), member(user("a", "Awa"))));

        assertThat(names(controller.mentions("KA", "l1", request))).containsExactly("Kader");
        assertThat(names(controller.mentions("", "l1", request))).containsExactly("Kader", "Awa");
    }

    @Test
    void pas_les_membres_d_une_ligue_dont_on_n_est_pas() {
        when(memberships.findByLeagueIdAndUserId("l1", "me")).thenReturn(Optional.empty());

        assertThat(controller.mentions("ka", "l1", request).getStatusCode().value()).isEqualTo(403);
        verify(memberships, never()).findByLeagueId(any());
    }
}
