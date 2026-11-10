package com.hooppicks.backendapplication.social;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SocialNotifierTest {

    private final BetRepository betRepository = mock(BetRepository.class);
    private final LeagueMessageRepository messageRepository = mock(LeagueMessageRepository.class);
    private final WeeklyTitleRepository titleRepository = mock(WeeklyTitleRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final ReactionNotifiedRepository notifiedRepository = mock(ReactionNotifiedRepository.class);
    private final PushService pushService = mock(PushService.class);
    private final SocialNotifier notifier = new SocialNotifier(betRepository, messageRepository, titleRepository, userRepository,
            notificationRepository, notifiedRepository, pushService);

    private final User kader = user("k", "Kader");
    private final User awa = user("a", "Awa");

    private static User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        return u;
    }

    private void betOf(User owner, String label) {
        BetSelection s = new BetSelection();
        s.setLabel(label);
        Bet b = new Bet();
        b.setUser(owner);
        b.getSelections().add(s);
        when(betRepository.findById("b1")).thenReturn(Optional.of(b));
    }

    private AppNotification sent() {
        ArgumentCaptor<AppNotification> saved = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void une_reponse_previent_l_auteur_du_message() {
        notifier.replied(kader, awa, "Bien vu", "/soiree");

        AppNotification n = sent();
        assertThat(n.getUser()).isEqualTo(awa);
        assertThat(n.getMessage()).isEqualTo("Kader a répondu à ton message : « Bien vu »");
        assertThat(n.getLink()).isEqualTo("/soiree");
    }

    @Test
    void on_n_est_pas_prevenu_de_sa_propre_reponse() {
        notifier.replied(kader, kader, "Bien vu", "/soiree");

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void celui_qui_a_repondu_et_tague_le_meme_joueur_ne_le_previent_qu_une_fois() {
        when(userRepository.findByUsernameIgnoreCase("Awa")).thenReturn(Optional.of(awa));

        notifier.mentions(kader, "@Awa bien vu", "/soiree", null, "a");

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void un_joueur_bloque_ne_previent_plus_personne() {
        UserBlockRepository blocks = mock(UserBlockRepository.class);
        when(blocks.existsById("a:k")).thenReturn(true);
        org.springframework.test.util.ReflectionTestUtils.setField(notifier, "blockRepository", blocks);
        when(userRepository.findByUsernameIgnoreCase("Awa")).thenReturn(Optional.of(awa));
        betOf(awa, "LAL (V)");

        notifier.replied(kader, awa, "Bien vu", "/soiree");
        notifier.mentions(kader, "@Awa", "/soiree", null);
        notifier.reacted(kader, "BET", "b1", "🔥");

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void une_reaction_previent_l_auteur_du_ticket() {
        betOf(awa, "LAL (V)");

        notifier.reacted(kader, "BET", "b1", "🔥");

        AppNotification n = sent();
        assertThat(n.getUser()).isEqualTo(awa);
        assertThat(n.getMessage()).isEqualTo("Kader a réagi 🔥 à ton ticket : LAL (V)");
        assertThat(n.getType()).isEqualTo(NotificationType.SOCIAL);
        assertThat(n.getLink()).isEqualTo("/bets");
        verify(pushService).sendToUser(anyString(), any());
    }

    @Test
    void reagir_a_son_propre_ticket_ne_notifie_personne() {
        betOf(kader, "LAL (V)");

        notifier.reacted(kader, "BET", "b1", "🔥");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void retirer_puis_remettre_sa_reaction_ne_renotifie_pas() {
        betOf(awa, "LAL (V)");
        when(notifiedRepository.existsById("k:BET:b1")).thenReturn(true);

        notifier.reacted(kader, "BET", "b1", "👍");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void pas_de_push_si_le_joueur_l_a_coupe_mais_la_cloche_reste() {
        awa.setNotifyLeagueActivity(false);
        betOf(awa, "LAL (V)");

        notifier.reacted(kader, "BET", "b1", "🔥");

        verify(notificationRepository).save(any());
        verify(pushService, never()).sendToUser(anyString(), any());
    }

    @Test
    void une_citation_previent_le_joueur_cite_avec_le_lien_du_fil() {
        when(userRepository.findByUsernameIgnoreCase("awa")).thenReturn(Optional.of(awa));

        notifier.mentions(kader, "Bien joué @awa.", "/leagues/l1", Set.of("a", "k"));

        AppNotification n = sent();
        assertThat(n.getUser()).isEqualTo(awa);
        assertThat(n.getMessage()).contains("Kader t'a cité").contains("Bien joué @awa.");
        assertThat(n.getLink()).isEqualTo("/leagues/l1");
    }

    @Test
    void on_ne_cite_pas_hors_de_la_ligue_ni_soi_meme_ni_une_adresse_mail() {
        when(userRepository.findByUsernameIgnoreCase("Awa")).thenReturn(Optional.of(awa));
        when(userRepository.findByUsernameIgnoreCase("Kader")).thenReturn(Optional.of(kader));

        notifier.mentions(kader, "@Awa et @Kader, ecris a moi@gmail.com", "/x", Set.of("k"));

        verify(notificationRepository, never()).save(any());
        verify(userRepository, never()).findByUsernameIgnoreCase("gmail.com");
    }

    @Test
    void un_message_long_est_coupe_pour_la_colonne_de_la_cloche() {
        when(userRepository.findByUsernameIgnoreCase("Awa")).thenReturn(Optional.of(awa));

        notifier.mentions(kader, "@Awa " + "x".repeat(275), "/x", null);

        assertThat(sent().getMessage().length()).isLessThanOrEqualTo(255);
    }

    @Test
    void cinq_citations_au_plus() {
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenAnswer(inv -> Optional.of(user(inv.getArgument(0), inv.getArgument(0))));

        notifier.mentions(kader, String.join(" ", List.of("@aa", "@bb", "@cc", "@dd", "@ee", "@ff", "@gg")), "/x", null);

        verify(notificationRepository, times(SocialNotifier.MAX_MENTIONS)).save(any());
    }
    @Test
    void un_pseudo_avec_une_espace_et_un_emoji_est_tague_en_entier() {
        User spidey = user("s", "Spidey \uD83D\uDD77");
        when(userRepository.findTop20ByUsernameStartingWithIgnoreCase("Spidey")).thenReturn(List.of(user("x", "Spid"), spidey));

        List<String> tagged = notifier.mentions(kader, "Bien vu @Spidey \uD83D\uDD77 !", "/soiree", null);

        assertThat(tagged).containsExactly("Spidey \uD83D\uDD77");
        verify(notificationRepository).save(any());
    }

    @Test
    void le_pseudo_le_plus_long_ecrit_en_entier_l_emporte() {
        User jean = user("j", "Jean");
        User jeanKader = user("jk", "Jean Kader");
        when(userRepository.findTop20ByUsernameStartingWithIgnoreCase("Jean")).thenReturn(List.of(jean, jeanKader));

        assertThat(notifier.mentions(kader, "@Jean Kader, @Jean Marc", "/x", null)).containsExactly("Jean Kader", "Jean");
    }

    @Test
    void un_tag_sur_un_pseudo_inconnu_ne_donne_rien() {
        assertThat(notifier.mentions(kader, "@Personne ici", "/x", null)).isEmpty();
        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }
}
