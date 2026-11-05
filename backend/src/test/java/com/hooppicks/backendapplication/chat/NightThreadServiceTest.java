package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.*;
import com.hooppicks.backendapplication.social.SocialNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NightThreadServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T22:00:00Z");

    private final NightMessageRepository messages = mock(NightMessageRepository.class);
    private final MessageReportRepository reports = mock(MessageReportRepository.class);
    private final UserBlockRepository blocks = mock(UserBlockRepository.class);
    private final ChatWordRepository words = mock(ChatWordRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BetRepository bets = mock(BetRepository.class);
    private final BetSelectionRepository selections = mock(BetSelectionRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final ActivityReactionRepository reactions = mock(ActivityReactionRepository.class);
    private final SocialNotifier notifier = mock(SocialNotifier.class);
    private final NightThreadService service = new NightThreadService(messages, reports, blocks, words, users, bets, selections,
            matches, reactions, notifier, Clock.fixed(NOW, BankrollService.ZONE));

    private final User me = user("me", "Moi");

    private static User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        return u;
    }

    @BeforeEach
    void setUp() {
        when(users.findById("me")).thenReturn(Optional.of(me));
        when(bets.findFirstByUser_Id("me")).thenReturn(Optional.of(new Bet()));
        when(words.findAll()).thenReturn(List.of());
        when(messages.findTopByUser_IdOrderByCreatedAtDesc("me")).thenReturn(Optional.empty());
    }

    private void refused(String text) {
        assertThatThrownBy(() -> service.post("me", text, null)).isInstanceOf(IllegalArgumentException.class);
        verify(messages, never()).save(any());
    }

    @Test
    void un_message_part_nettoye_sur_la_soiree_en_cours_et_cite() {
        service.post("me", "  Allez @Kader  ", null);

        ArgumentCaptor<NightMessage> saved = ArgumentCaptor.forClass(NightMessage.class);
        verify(messages).save(saved.capture());
        assertThat(saved.getValue().getText()).isEqualTo("Allez @Kader");
        assertThat(saved.getValue().getNight()).isEqualTo(LocalDate.of(2026, 11, 6));
        verify(notifier).mentions(me, "Allez @Kader", "/soiree", null);
    }

    @Test
    void avant_midi_on_est_encore_sur_la_soiree_de_la_veille() {
        assertThat(NightThreadService.nightOf(Instant.parse("2026-11-07T03:00:00Z"))).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(NightThreadService.nightOf(Instant.parse("2026-11-07T13:00:00Z"))).isEqualTo(LocalDate.of(2026, 11, 7));
    }

    @Test
    void liens_refuses() {
        refused("va voir https://arnaque.test");
        refused("www.arnaque.test");
        refused("Regarde monsite.com vite");
        refused("bit.ly/abc");
    }

    @Test
    void une_phrase_avec_un_point_normal_passe() {
        service.post("me", "Bien joué.Vraiment 3.5 points", null);
        verify(messages).save(any());
    }

    @Test
    void vide_ou_trop_long_refuse() {
        refused("   ");
        refused("x".repeat(281));
    }

    @Test
    void sans_ticket_pose_on_ne_peut_pas_ecrire() {
        when(bets.findFirstByUser_Id("me")).thenReturn(Optional.empty());
        refused("Salut");
    }

    @Test
    void parole_coupee_tant_que_le_delai_court() {
        me.setChatMutedUntil(NOW.plusSeconds(60));
        refused("Salut");

        me.setChatMutedUntil(NOW.minusSeconds(60));
        service.post("me", "Salut", null);
        verify(messages).save(any());
    }

    @Test
    void un_message_toutes_les_3_secondes() {
        NightMessage last = new NightMessage();
        last.setCreatedAt(NOW.minusSeconds(2));
        when(messages.findTopByUser_IdOrderByCreatedAtDesc("me")).thenReturn(Optional.of(last));
        refused("Encore");

        last.setCreatedAt(NOW.minusSeconds(3));
        service.post("me", "Encore", null);
        verify(messages).save(any());
    }

    @Test
    void jamais_deux_fois_le_meme_message_d_affilee() {
        NightMessage last = new NightMessage();
        last.setCreatedAt(NOW.minusSeconds(60));
        last.setText("Allez les Knicks");
        when(messages.findTopByUser_IdOrderByCreatedAtDesc("me")).thenReturn(Optional.of(last));
        refused("ALLEZ les knicks");
    }

    @Test
    void mots_refuses_entiers_sans_accents_ni_majuscules() {
        assertThat(NightThreadService.containsBannedWord("Espèce de CRÉTIN !", List.of("cretin"))).isTrue();
        // Mot entier : « con » ne bloque pas « contre ».
        assertThat(NightThreadService.containsBannedWord("Je joue contre eux", List.of("con"))).isFalse();
        assertThat(NightThreadService.containsBannedWord("bande de con,toi", List.of("con"))).isTrue();
        assertThat(NightThreadService.containsBannedWord("p.u.t.e", List.of("pute"))).isTrue();
        assertThat(NightThreadService.containsBannedWord("Des connards !", List.of("connard"))).isTrue();
        assertThat(NightThreadService.containsBannedWord("espèce de SALE noir", List.of("sale noir"))).isTrue();
    }

    @Test
    void la_liste_de_depart_laisse_passer_les_messages_de_basket() {
        for (String ok : List.of("Quel contre de Wemby !", "Il est en retard sur la défense", "Belle passe décisive",
                "Le noir de leur maillot est sale", "Nul ce match", "Quelle bête ce Giannis", "Ce gaou a pris les Nets")) {
            assertThat(NightThreadService.containsBannedWord(ok, ChatWordSeeder.DEFAULT_WORDS)).as(ok).isFalse();
        }
        assertThat(NightThreadService.containsBannedWord("Ferme-la connard", ChatWordSeeder.DEFAULT_WORDS)).isTrue();
    }

    @Test
    void liste_de_depart_posee_une_seule_fois() {
        new ChatWordSeeder(words).seed();
        verify(words).saveAll(any());

        clearInvocations(words);
        when(words.count()).thenReturn(1L);
        new ChatWordSeeder(words).seed();
        verify(words, never()).saveAll(any());
    }

    private NightMessage messageOf(User author) {
        NightMessage m = new NightMessage();
        m.setId("m1");
        m.setUser(author);
        when(messages.findById("m1")).thenReturn(Optional.of(m));
        return m;
    }

    @Test
    void trois_joueurs_differents_masquent_le_message() {
        NightMessage m = messageOf(user("autre", "Autre"));
        when(reports.existsById(anyString())).thenReturn(false);
        when(reports.countByMessageId("m1")).thenReturn(2L);
        service.report("me", "m1");
        assertThat(m.getHidden()).isNull();

        when(reports.countByMessageId("m1")).thenReturn(3L);
        service.report("me", "m1");
        assertThat(m.getHidden()).isTrue();
    }

    @Test
    void un_message_retabli_par_un_admin_ne_se_masque_plus() {
        NightMessage m = messageOf(user("autre", "Autre"));
        m.setReviewed(true);

        service.report("me", "m1");

        verify(reports, never()).saveAndFlush(any());
        assertThat(m.getHidden()).isNull();
    }

    @Test
    void on_ne_signale_ni_son_message_ni_un_message_automatique() {
        messageOf(me);
        assertThatThrownBy(() -> service.report("me", "m1")).isInstanceOf(IllegalArgumentException.class);
        messageOf(null);
        assertThatThrownBy(() -> service.report("me", "m1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void on_ne_supprime_que_ses_propres_messages() {
        messageOf(user("autre", "Autre"));
        when(reactions.findByTargetTypeInAndTargetIdIn(any(), any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.delete("me", "m1")).isInstanceOf(IllegalStateException.class);

        NightMessage mine = messageOf(me);
        service.delete("me", "m1");
        verify(messages).delete(mine);
    }

    // --- Fil ---

    private Match match(MatchStatus status, Instant date) {
        Team home = new Team();
        home.setAbbreviation("CLE");
        Team away = new Team();
        away.setAbbreviation("BKN");
        Match m = new Match();
        m.setId("x");
        m.setStatus(status);
        m.setDate(date);
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        return m;
    }

    private NightMessage posted(User author, String matchId) {
        NightMessage m = new NightMessage();
        m.setId("m-" + author.getId());
        m.setUser(author);
        m.setMatchId(matchId);
        m.setText("Allez");
        m.setCreatedAt(NOW);
        return m;
    }

    private void campOf(User author, String label) {
        Bet bet = new Bet();
        bet.setUser(author);
        BetSelection s = new BetSelection();
        s.setBet(bet);
        s.setLabel(label);
        when(selections.findFriendsOnMatch(eq("x"), any())).thenReturn(List.of(s));
    }

    @Test
    void le_camp_reste_cache_avant_le_coup_d_envoi() {
        User kader = user("k", "Kader");
        when(messages.findTop100ByMatchIdOrderByCreatedAtDesc("x")).thenReturn(List.of(posted(kader, "x")));
        when(matches.findAllById(any())).thenReturn(List.of(match(MatchStatus.SCHEDULED, NOW.plusSeconds(3600))));
        campOf(kader, "CLE -8,5");

        NightThreadService.Message m = service.feed("me", "x").messages().get(0);

        assertThat(m.camp()).isNull();
        assertThat(m.matchLabel()).isEqualTo("BKN @ CLE");
    }

    @Test
    void le_camp_s_affiche_une_fois_le_match_commence() {
        User kader = user("k", "Kader");
        when(messages.findTop100ByMatchIdOrderByCreatedAtDesc("x")).thenReturn(List.of(posted(kader, "x")));
        when(matches.findAllById(any())).thenReturn(List.of(match(MatchStatus.LIVE, NOW.minusSeconds(600))));
        campOf(kader, "CLE -8,5");

        assertThat(service.feed("me", "x").messages().get(0).camp()).isEqualTo("CLE -8,5");
    }

    @Test
    void messages_masques_et_joueurs_bloques_n_apparaissent_pas() {
        User visible = user("v", "Visible");
        User bloque = user("b", "Bloque");
        NightMessage masque = posted(user("h", "Masque"), null);
        masque.setHidden(true);
        when(messages.findTop100ByNightAndDebateDayIsNullOrderByCreatedAtDesc(any())).thenReturn(List.of(posted(visible, null), posted(bloque, null), masque));
        UserBlock block = new UserBlock();
        block.setBlockedId("b");
        when(blocks.findByBlockerId("me")).thenReturn(List.of(block));

        assertThat(service.feed("me", null).messages()).extracting(NightThreadService.Message::username).containsExactly("Visible");
    }

    @Test
    void on_n_ecrit_que_sur_un_match_de_la_soiree_en_cours() {
        when(matches.findById("x")).thenReturn(Optional.of(match(MatchStatus.FINISHED, NOW.minusSeconds(3 * 86400))));
        assertThatThrownBy(() -> service.post("me", "Salut", "x")).isInstanceOf(IllegalArgumentException.class);

        when(matches.findById("x")).thenReturn(Optional.of(match(MatchStatus.SCHEDULED, NOW.plusSeconds(3600))));
        service.post("me", "Salut", "x");
        verify(messages).save(any());
    }

    @Test
    void un_argument_de_debat_suit_les_memes_regles_et_garde_son_jour() {
        LocalDate day = LocalDate.of(2026, 11, 6);
        assertThatThrownBy(() -> service.postDebate("me", "va sur monsite.com", day)).isInstanceOf(IllegalArgumentException.class);

        service.postDebate("me", "Jordan @Kader", day);

        ArgumentCaptor<NightMessage> saved = ArgumentCaptor.forClass(NightMessage.class);
        verify(messages).save(saved.capture());
        assertThat(saved.getValue().getDebateDay()).isEqualTo(day);
        verify(notifier).mentions(me, "Jordan @Kader", "/debat", null);
    }

    @Test
    void dans_un_debat_le_camp_remplace_ce_que_le_joueur_a_joue() {
        User kader = user("k", "Kader");
        LocalDate day = LocalDate.of(2026, 11, 6);
        when(messages.findTop100ByDebateDayOrderByCreatedAtDesc(day)).thenReturn(List.of(posted(kader, null)));

        NightThreadService.Feed feed = service.debateFeed("me", day, java.util.Map.of("k", "Jordan"), null);

        assertThat(feed.messages().get(0).camp()).isEqualTo("Jordan");
        assertThat(feed.cannotWrite()).isNull();
    }

    @Test
    void on_ne_peut_pas_se_bloquer_soi_meme() {
        when(users.findByUsernameIgnoreCase("Moi")).thenReturn(Optional.of(me));
        assertThatThrownBy(() -> service.block("me", "Moi")).isInstanceOf(IllegalArgumentException.class);
    }
}
