package com.hooppicks.backendapplication.avatar;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.palmares.PalmaresService;
import com.hooppicks.backendapplication.repository.PhaseTitleRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WeeklyTitleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AvatarServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final WeeklyTitleRepository weeklyTitleRepository = mock(WeeklyTitleRepository.class);
    private final PhaseTitleRepository phaseTitleRepository = mock(PhaseTitleRepository.class);
    private final AvatarService service = new AvatarService(userRepository, weeklyTitleRepository, phaseTitleRepository);

    private static User user(String username, String team) {
        User u = new User();
        u.setId("id-" + username);
        u.setUsername(username);
        u.setFavoriteTeam(team);
        return u;
    }

    @Test
    void maillot_par_defaut_aux_couleurs_de_l_equipe_et_stable() {
        User a = user("Kader", "Los Angeles Lakers");
        AvatarService.applyDefaults(a);
        User again = user("Kader", "Los Angeles Lakers");
        AvatarService.applyDefaults(again);

        assertThat(a.getAvatarColorway()).isEqualTo("purple");
        assertThat(a.getAvatarNumber()).isBetween(1, 99);
        assertThat(AvatarService.POSITIONS).contains(a.getAvatarPosition());
        assertThat(a.getAvatarIcon()).isNotEqualTo(AvatarService.CROWN);
        assertThat(again.getAvatarNumber()).isEqualTo(a.getAvatarNumber());
        assertThat(again.getAvatarIcon()).isEqualTo(a.getAvatarIcon());
    }

    @Test
    void les_joueurs_n_ont_plus_tous_le_meme_maillot() {
        List<String> names = List.of("Kader", "Awa3pts", "DunkMaster", "Mamba225", "Fatou_B", "YaoSplash", "SkyHook", "Nana_K");
        long distinct = names.stream().map(n -> {
            User u = user(n, "");
            AvatarService.applyDefaults(u);
            return u.getAvatarNumber() + u.getAvatarColorway();
        }).distinct().count();

        assertThat(distinct).isGreaterThan(5);
    }

    @Test
    void le_maillot_jamais_touche_suit_l_equipe_favorite_pas_le_maillot_choisi() {
        User untouched = user("Kader", "Boston Celtics");
        AvatarService.followFavoriteTeam(untouched);
        assertThat(untouched.getAvatarColorway()).isEqualTo("green");

        User customized = user("Awa", "Boston Celtics");
        customized.setAvatarColorway("orange");
        customized.setAvatarCustomized(true);
        AvatarService.followFavoriteTeam(customized);
        assertThat(customized.getAvatarColorway()).isEqualTo("orange");
    }

    @Test
    void couronne_reservee_aux_joueurs_titres() {
        when(weeklyTitleRepository.existsByUserIdAndWeekGreaterThanEqual("champ", PalmaresService.FIRST_WEEK)).thenReturn(true);
        when(phaseTitleRepository.existsByUserId("cup")).thenReturn(true);

        assertThat(service.canUseIcon("champ", "crown")).isTrue();
        assertThat(service.canUseIcon("cup", "crown")).isTrue();
        assertThat(service.canUseIcon("nobody", "crown")).isFalse();
        assertThat(service.canUseIcon("nobody", "three")).isTrue();
        assertThat(service.canUseIcon("nobody", "laser")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void rattrapage_seulement_des_maillots_n0_orange_jamais_touches() {
        User legacy = user("Kader", "Miami Heat");
        User chosen = user("Awa", "");
        chosen.setAvatarCustomized(true);
        User other = user("Nana", "");
        other.setAvatarNumber(23);
        when(userRepository.findAll()).thenReturn(List.of(legacy, chosen, other));

        service.backfillDefaults();

        assertThat(legacy.getAvatarColorway()).isEqualTo("red");
        assertThat(legacy.getAvatarNumber()).isPositive();
        assertThat(chosen.getAvatarNumber()).isZero();
        assertThat(other.getAvatarNumber()).isEqualTo(23);
        verify(userRepository).saveAll(argThat(list -> ((List<User>) list).size() == 1));
    }

    @Test
    void rien_a_rattraper_aucune_ecriture() {
        when(userRepository.findAll()).thenReturn(List.of());
        service.backfillDefaults();
        verify(userRepository, never()).saveAll(anyList());
    }
}
