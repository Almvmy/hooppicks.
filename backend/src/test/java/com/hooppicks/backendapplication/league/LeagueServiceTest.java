package com.hooppicks.backendapplication.league;

import com.hooppicks.backendapplication.dto.LeagueDto;
import com.hooppicks.backendapplication.entity.League;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.entity.User;
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

    private LeagueService leagueService;

    @BeforeEach
    void setUp() {
        leagueService = new LeagueService(leagueRepository, membershipRepository, userRepository, betRepository,
                notificationRepository);
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
}
