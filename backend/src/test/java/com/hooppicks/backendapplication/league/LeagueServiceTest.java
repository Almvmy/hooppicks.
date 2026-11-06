package com.hooppicks.backendapplication.league;


import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.dto.LeagueActivityDto;
import com.hooppicks.backendapplication.dto.LeagueDto;
import com.hooppicks.backendapplication.entity.ActivityReaction;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.League;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.ActivityReactionRepository;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private LeagueMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BetRepository betRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ActivityReactionRepository activityReactionRepository;
    @Mock
    private PushService pushService;

    private LeagueService leagueService;

    @BeforeEach
    void setUp() {
        leagueService = new LeagueService(leagueRepository, membershipRepository, userRepository, betRepository,
                notificationRepository, activityReactionRepository, pushService);
    }

    @Test
    void creer_une_ligue_genere_un_code_valide_et_ajoute_le_createur_comme_membre() {
        when(leagueRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User()));
        when(membershipRepository.countByLeagueId(any())).thenReturn(1L);

        LeagueDto dto = leagueService.createLeague("owner-1", "Les Requins du Parquet");

        ArgumentCaptor<League> leagueCaptor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(leagueCaptor.capture());
        League saved = leagueCaptor.getValue();

        assertThat(saved.getInviteCode()).hasSize(6);
        assertThat(saved.getInviteCode()).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}");
        assertThat(saved.getOwnerId()).isEqualTo("owner-1");

        verify(membershipRepository).save(any(LeagueMembership.class));
        assertThat(dto.isOwner()).isTrue();
        assertThat(dto.name()).isEqualTo("Les Requins du Parquet");
    }

    @Test
    void rejoindre_une_ligue_deja_rejointe_ne_cree_pas_une_deuxieme_adhesion() {
        League league = new League();
        league.setId("league-1");
        league.setInviteCode("ABC123");
        league.setOwnerId("owner-1");

        when(leagueRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(league));
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-2"))
                .thenReturn(Optional.of(new LeagueMembership()));
        when(membershipRepository.countByLeagueId("league-1")).thenReturn(2L);

        leagueService.joinLeague("user-2", "abc123");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void rejoindre_pour_la_premiere_fois_cree_bien_une_adhesion() {
        League league = new League();
        league.setId("league-1");
        league.setInviteCode("ABC123");
        league.setOwnerId("owner-1");

        when(leagueRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(league));
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-2")).thenReturn(Optional.empty());
        when(userRepository.findById("user-2")).thenReturn(Optional.of(new User()));
        when(membershipRepository.countByLeagueId("league-1")).thenReturn(2L);

        leagueService.joinLeague("user-2", "ABC123");

        verify(membershipRepository).save(any(LeagueMembership.class));
    }

    @Test
    void rejoindre_notifie_les_membres_existants_mais_pas_le_nouvel_arrivant() {
        League league = new League();
        league.setId("league-1");
        league.setInviteCode("ABC123");
        league.setOwnerId("owner-1");
        league.setName("Les Requins du Parquet");

        User joiningUser = new User();
        joiningUser.setId("user-2");
        joiningUser.setUsername("nouveau");

        User existingMember = new User();
        existingMember.setId("owner-1");
        LeagueMembership existingMembership = new LeagueMembership();
        existingMembership.setUser(existingMember);

        LeagueMembership newMembership = new LeagueMembership();
        newMembership.setUser(joiningUser);

        when(leagueRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(league));
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-2")).thenReturn(Optional.empty());
        when(userRepository.findById("user-2")).thenReturn(Optional.of(joiningUser));
        when(membershipRepository.findByLeagueId("league-1"))
                .thenReturn(List.of(existingMembership, newMembership));
        when(membershipRepository.countByLeagueId("league-1")).thenReturn(2L);

        leagueService.joinLeague("user-2", "ABC123");

        ArgumentCaptor<com.hooppicks.backendapplication.entity.AppNotification> captor =
                ArgumentCaptor.forClass(com.hooppicks.backendapplication.entity.AppNotification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(existingMember);
        assertThat(captor.getValue().getMessage()).contains("nouveau").contains("Les Requins du Parquet");
    }

    @Test
    void un_code_invalide_leve_une_exception() {
        when(leagueRepository.findByInviteCode("ZZZZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.joinLeague("user-2", "ZZZZZZ"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_classement_est_refuse_a_qui_n_est_pas_membre() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "intrus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.getLeagueLeaderboard("league-1", "intrus"))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(betRepository);
    }

    @Test
    void quitter_une_ligue_qui_devient_vide_supprime_la_ligue() {
        League league = new League();
        league.setId("league-1");
        league.setOwnerId("seul-membre");
        LeagueMembership membership = new LeagueMembership();
        membership.setLeague(league);
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "seul-membre"))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.findByLeagueId("league-1")).thenReturn(List.of());

        leagueService.leaveLeague("league-1", "seul-membre");

        verify(membershipRepository).delete(membership);
        verify(leagueRepository).deleteById("league-1");
    }

    @Test
    void quitter_une_ligue_qui_a_encore_des_membres_ne_la_supprime_pas() {
        League league = new League();
        league.setId("league-1");
        league.setOwnerId("owner-1"); // pas celui qui quitte
        LeagueMembership membership = new LeagueMembership();
        membership.setLeague(league);
        LeagueMembership remaining = new LeagueMembership();
        remaining.setUser(userWithId("owner-1"));
        remaining.setJoinedAt(Instant.now());
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.findByLeagueId("league-1")).thenReturn(List.of(remaining));

        leagueService.leaveLeague("league-1", "user-1");

        verify(membershipRepository).delete(membership);
        verify(leagueRepository, never()).deleteById(anyString());
        verify(leagueRepository, never()).save(any());
    }

    @Test
    void quand_le_proprietaire_quitte_la_propriete_passe_au_membre_le_plus_ancien() {
        League league = new League();
        league.setId("league-1");
        league.setOwnerId("owner-1");
        LeagueMembership ownerMembership = new LeagueMembership();
        ownerMembership.setLeague(league);

        LeagueMembership older = new LeagueMembership();
        older.setUser(userWithId("membre-ancien"));
        older.setJoinedAt(Instant.parse("2026-01-01T00:00:00Z"));
        LeagueMembership newer = new LeagueMembership();
        newer.setUser(userWithId("membre-recent"));
        newer.setJoinedAt(Instant.parse("2026-02-01T00:00:00Z"));

        when(membershipRepository.findByLeagueIdAndUserId("league-1", "owner-1"))
                .thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.findByLeagueId("league-1")).thenReturn(List.of(newer, older));

        leagueService.leaveLeague("league-1", "owner-1");

        assertThat(league.getOwnerId()).isEqualTo("membre-ancien");
        verify(leagueRepository).save(league);
        verify(leagueRepository, never()).deleteById(anyString());
    }

    private User userWithId(String id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void le_fil_d_activite_affiche_les_paris_en_cours_pas_seulement_les_gains() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(new LeagueMembership()));
        when(membershipRepository.findByLeagueId("league-1")).thenReturn(List.of());

        User bettor = userWithId("user-2");
        bettor.setUsername("Marc");
        Bet pending = new Bet();
        pending.setId("bet-1");
        pending.setUser(bettor);
        pending.setStake(150);
        pending.setStatus(BetStatus.PENDING);
        pending.setPlacedAt(Instant.now());
        BetSelection selection = new BetSelection();
        selection.setLabel("Knicks (V)");
        pending.setSelections(List.of(selection));

        when(betRepository.findTop10ByUser_IdInAndStatusOrderByPlacedAtDesc(any(), eq(BetStatus.PENDING)))
                .thenReturn(List.of(pending));
        when(betRepository.findTop10ByUser_IdInAndStatusOrderByResolvedAtDesc(any(), eq(BetStatus.WON)))
                .thenReturn(List.of());
        when(activityReactionRepository.findByTargetTypeInAndTargetIdIn(any(), any())).thenReturn(List.of());

        List<LeagueActivityDto> activity = leagueService.getRecentActivity("league-1", "user-1");

        assertThat(activity).hasSize(1);
        assertThat(activity.get(0).message()).isEqualTo("a misé 150 pts sur Knicks (V)");
        assertThat(activity.get(0).targetType()).isEqualTo("BET");
        assertThat(activity.get(0).targetId()).isEqualTo("bet-1");
    }

    @Test
    void un_pari_gagne_sans_resolvedAt_ne_plante_pas_le_fil_et_retombe_sur_placedAt() {
        // Régression : des paris WON existants n'ont pas tous resolvedAt
        // renseigné (résolution manuelle ancienne côté admin) : le tri du fil
        // plantait dessus avec une NullPointerException plutôt que d'afficher
        // l'item avec une date de repli.
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(new LeagueMembership()));
        when(membershipRepository.findByLeagueId("league-1")).thenReturn(List.of());
        when(betRepository.findTop10ByUser_IdInAndStatusOrderByPlacedAtDesc(any(), eq(BetStatus.PENDING)))
                .thenReturn(List.of());

        User bettor = userWithId("user-2");
        bettor.setUsername("Marc");
        Bet wonWithoutResolvedAt = new Bet();
        wonWithoutResolvedAt.setId("bet-2");
        wonWithoutResolvedAt.setUser(bettor);
        wonWithoutResolvedAt.setPotentialPayout(300);
        wonWithoutResolvedAt.setStatus(BetStatus.WON);
        wonWithoutResolvedAt.setPlacedAt(Instant.parse("2026-07-30T10:00:00Z"));
        wonWithoutResolvedAt.setResolvedAt(null);

        when(betRepository.findTop10ByUser_IdInAndStatusOrderByResolvedAtDesc(any(), eq(BetStatus.WON)))
                .thenReturn(List.of(wonWithoutResolvedAt));
        when(activityReactionRepository.findByTargetTypeInAndTargetIdIn(any(), any())).thenReturn(List.of());

        List<LeagueActivityDto> activity = leagueService.getRecentActivity("league-1", "user-1");

        assertThat(activity).hasSize(1);
        assertThat(activity.get(0).occurredAt()).isEqualTo(Instant.parse("2026-07-30T10:00:00Z"));
    }

    @Test
    void reagir_une_premiere_fois_cree_la_reaction() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(new LeagueMembership()));
        when(activityReactionRepository.findByTargetTypeAndTargetIdAndUser_Id("BET", "bet-1", "user-1"))
                .thenReturn(List.of());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userWithId("user-1")));

        leagueService.toggleReaction("league-1", "user-1", "BET", "bet-1", "🔥");

        ArgumentCaptor<ActivityReaction> saved = ArgumentCaptor.forClass(ActivityReaction.class);
        verify(activityReactionRepository).save(saved.capture());
        assertThat(saved.getValue().getEmoji()).isEqualTo("🔥");
    }

    @Test
    void changer_de_reaction_remplace_l_ancienne() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(new LeagueMembership()));
        ActivityReaction thumbsUp = reaction("👍");
        when(activityReactionRepository.findByTargetTypeAndTargetIdAndUser_Id("BET", "bet-1", "user-1"))
                .thenReturn(List.of(thumbsUp));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userWithId("user-1")));

        leagueService.toggleReaction("league-1", "user-1", "BET", "bet-1", "👎");

        verify(activityReactionRepository).deleteAll(List.of(thumbsUp));
        ArgumentCaptor<ActivityReaction> saved = ArgumentCaptor.forClass(ActivityReaction.class);
        verify(activityReactionRepository).save(saved.capture());
        assertThat(saved.getValue().getEmoji()).isEqualTo("👎");
    }

    @Test
    void des_doublons_anterieurs_sont_nettoyes_au_prochain_clic() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(new LeagueMembership()));
        List<ActivityReaction> legacy = List.of(reaction("👍"), reaction("🔥"), reaction("👎"));
        when(activityReactionRepository.findByTargetTypeAndTargetIdAndUser_Id("BET", "bet-1", "user-1"))
                .thenReturn(legacy);

        leagueService.toggleReaction("league-1", "user-1", "BET", "bet-1", "🔥");

        // 🔥 était déjà là : tout est retiré, rien n'est recréé.
        verify(activityReactionRepository).deleteAll(legacy);
        verify(activityReactionRepository, never()).save(any());
    }

    private static ActivityReaction reaction(String emoji) {
        ActivityReaction r = new ActivityReaction();
        r.setEmoji(emoji);
        return r;
    }

    @Test
    void reagir_une_deuxieme_fois_avec_le_meme_emoji_retire_la_reaction() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(new LeagueMembership()));
        ActivityReaction existing = reaction("🔥");
        when(activityReactionRepository.findByTargetTypeAndTargetIdAndUser_Id("BET", "bet-1", "user-1"))
                .thenReturn(List.of(existing));

        leagueService.toggleReaction("league-1", "user-1", "BET", "bet-1", "🔥");

        verify(activityReactionRepository).deleteAll(List.of(existing));
        verify(activityReactionRepository, never()).save(any());
    }

    @Test
    void un_emoji_hors_liste_est_refuse() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1"))
                .thenReturn(Optional.of(new LeagueMembership()));

        assertThatThrownBy(() -> leagueService.toggleReaction("league-1", "user-1", "BET", "bet-1", "💩"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(activityReactionRepository);
    }

    @Test
    void reagir_est_refuse_a_qui_n_est_pas_membre_de_la_ligue() {
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "intrus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService.toggleReaction("league-1", "intrus", "BET", "bet-1", "🔥"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(activityReactionRepository);
    }

    // --- Fil de la ligue ---

    private final com.hooppicks.backendapplication.repository.LeagueMessageRepository messageRepository =
            mock(com.hooppicks.backendapplication.repository.LeagueMessageRepository.class);
    private final com.hooppicks.backendapplication.repository.MatchRepository matchRepository =
            mock(com.hooppicks.backendapplication.repository.MatchRepository.class);

    private void wireThread() {
        org.springframework.test.util.ReflectionTestUtils.setField(leagueService, "messageRepository", messageRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(leagueService, "matchRepository", matchRepository);
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "user-1")).thenReturn(Optional.of(new LeagueMembership()));
    }

    @Test
    void un_message_est_nettoye_et_enregistre() {
        wireThread();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userWithId("user-1")));

        leagueService.postMessage("league-1", "user-1", "  On y croit  ");

        ArgumentCaptor<com.hooppicks.backendapplication.entity.LeagueMessage> saved =
                ArgumentCaptor.forClass(com.hooppicks.backendapplication.entity.LeagueMessage.class);
        verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().getText()).isEqualTo("On y croit");
        assertThat(saved.getValue().getLeagueId()).isEqualTo("league-1");
    }

    private com.hooppicks.backendapplication.entity.LeagueMessage leagueMessage(String id, String leagueId, User author) {
        com.hooppicks.backendapplication.entity.LeagueMessage m = new com.hooppicks.backendapplication.entity.LeagueMessage();
        m.setId(id);
        m.setLeagueId(leagueId);
        m.setUser(author);
        m.setText("On y croit");
        when(messageRepository.findById(id)).thenReturn(Optional.of(m));
        return m;
    }

    @Test
    void repondre_a_un_message_de_la_ligue_garde_le_lien() {
        wireThread();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userWithId("user-1")));
        leagueMessage("p", "league-1", userWithId("user-2"));

        leagueService.postMessage("league-1", "user-1", "Moi aussi", "p");

        ArgumentCaptor<com.hooppicks.backendapplication.entity.LeagueMessage> saved =
                ArgumentCaptor.forClass(com.hooppicks.backendapplication.entity.LeagueMessage.class);
        verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().getReplyToId()).isEqualTo("p");
    }

    @Test
    void on_ne_repond_pas_a_un_message_d_une_autre_ligue() {
        wireThread();
        leagueMessage("p", "league-2", userWithId("user-2"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> leagueService.postMessage("league-1", "user-1", "Moi aussi", "p"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(messageRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void message_vide_trop_long_ou_trop_de_messages_refuse() {
        wireThread();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> leagueService.postMessage("league-1", "user-1", "   "))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> leagueService.postMessage("league-1", "user-1", "x".repeat(281)))
                .isInstanceOf(IllegalArgumentException.class);

        when(messageRepository.countByUser_IdAndCreatedAtAfter(eq("user-1"), any())).thenReturn(30L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> leagueService.postMessage("league-1", "user-1", "encore"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void un_non_membre_ne_peut_pas_ecrire() {
        org.springframework.test.util.ReflectionTestUtils.setField(leagueService, "messageRepository", messageRepository);
        when(membershipRepository.findByLeagueIdAndUserId("league-1", "intrus")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> leagueService.postMessage("league-1", "intrus", "salut"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void supprimer_le_message_d_un_autre_est_refuse_sauf_au_createur_de_la_ligue() {
        org.springframework.test.util.ReflectionTestUtils.setField(leagueService, "messageRepository", messageRepository);
        com.hooppicks.backendapplication.entity.LeagueMessage message = new com.hooppicks.backendapplication.entity.LeagueMessage();
        message.setId("m1");
        message.setLeagueId("league-1");
        message.setUser(userWithId("auteur"));
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));
        League league = new League();
        league.setOwnerId("chef");
        when(leagueRepository.findById("league-1")).thenReturn(Optional.of(league));
        when(activityReactionRepository.findByTargetTypeInAndTargetIdIn(any(), any())).thenReturn(List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> leagueService.deleteMessage("league-1", "voisin", "m1"))
                .isInstanceOf(IllegalStateException.class);
        verify(messageRepository, never()).delete(any());

        leagueService.deleteMessage("league-1", "chef", "m1");
        verify(messageRepository).delete(message);
    }

    private Bet pendingBet(String ownerId, String matchId) {
        Bet bet = new Bet();
        bet.setId("bet-1");
        bet.setUser(userWithId(ownerId));
        bet.setStake(50);
        bet.setStatus(BetStatus.PENDING);
        bet.setPlacedAt(Instant.now());
        BetSelection s = new BetSelection();
        s.setMatchId(matchId);
        s.setMarket("moneyline");
        s.setOutcome("home");
        s.setLabel("Knicks (V)");
        s.setOdds(1.8);
        bet.setSelections(List.of(s));
        return bet;
    }

    private com.hooppicks.backendapplication.entity.Match match(com.hooppicks.backendapplication.entity.MatchStatus status, Instant date) {
        com.hooppicks.backendapplication.entity.Match m = new com.hooppicks.backendapplication.entity.Match();
        m.setStatus(status);
        m.setDate(date);
        return m;
    }

    private List<LeagueActivityDto> feedWith(Bet bet) {
        wireThread();
        when(membershipRepository.findByLeagueId("league-1")).thenReturn(List.of());
        when(betRepository.findTop10ByUser_IdInAndStatusOrderByPlacedAtDesc(any(), eq(BetStatus.PENDING))).thenReturn(List.of(bet));
        when(betRepository.findTop10ByUser_IdInAndStatusOrderByResolvedAtDesc(any(), eq(BetStatus.WON))).thenReturn(List.of());
        when(activityReactionRepository.findByTargetTypeInAndTargetIdIn(any(), any())).thenReturn(List.of());
        when(leagueRepository.findById("league-1")).thenReturn(Optional.empty());
        when(messageRepository.findTop30ByLeagueIdOrderByCreatedAtDesc("league-1")).thenReturn(List.of());
        return leagueService.getRecentActivity("league-1", "user-1");
    }

    @Test
    void je_joue_pareil_seulement_sur_le_ticket_d_un_autre_avant_le_coup_d_envoi() {
        when(matchRepository.findAllById(any())).thenReturn(List.of(match(com.hooppicks.backendapplication.entity.MatchStatus.SCHEDULED, Instant.now().plusSeconds(3600))));
        assertThat(feedWith(pendingBet("user-2", "m1")).get(0).selections()).hasSize(1);
        // Son propre ticket : rien à reprendre.
        assertThat(feedWith(pendingBet("user-1", "m1")).get(0).selections()).isEmpty();

        when(matchRepository.findAllById(any())).thenReturn(List.of(match(com.hooppicks.backendapplication.entity.MatchStatus.SCHEDULED, Instant.now().minusSeconds(60))));
        assertThat(feedWith(pendingBet("user-2", "m1")).get(0).selections()).isEmpty();
    }
}
