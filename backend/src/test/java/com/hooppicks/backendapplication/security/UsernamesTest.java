package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UsernamesTest {

    private final UserRepository repository = mock(UserRepository.class);

    private static User user(String id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    @Test
    void les_espaces_en_trop_et_invisibles_partent() {
        assertThat(Usernames.normalize("  Spidey \uD83D\uDD77 ")).isEqualTo("Spidey \uD83D\uDD77");
        assertThat(Usernames.normalize("Jean\u00A0\u00A0Kader\u200B")).isEqualTo("Jean Kader");
        assertThat(Usernames.normalize(null)).isEmpty();
    }

    @Test
    void longueur_comptee_en_caracteres_et_caracteres_interdits() {
        assertThat(Usernames.invalidReason("ab")).isNotNull();
        assertThat(Usernames.invalidReason("Spidey \uD83D\uDD77")).isNull();
        assertThat(Usernames.invalidReason("a".repeat(21))).isNotNull();
        assertThat(Usernames.invalidReason("moi@lui")).isNotNull();
        assertThat(Usernames.invalidReason("un/deux")).isNotNull();
    }

    @Test
    void un_compte_ancien_avec_son_espace_en_trop_se_retrouve_par_son_nom_propre() {
        User old = user("u1", "Kader ");
        when(repository.findByUsername("Kader ")).thenReturn(Optional.of(old));
        when(repository.findByUsername("Kader")).thenReturn(Optional.empty());

        assertThat(Usernames.find(repository, "Kader ")).contains(old);
        assertThat(Usernames.find(repository, "Kader")).isEmpty();
    }

    @Test
    void un_nom_tape_avec_une_espace_retrouve_le_compte_propre() {
        User kader = user("u1", "Kader");
        when(repository.findByUsername("Kader ")).thenReturn(Optional.empty());
        when(repository.findByUsername("Kader")).thenReturn(Optional.of(kader));

        assertThat(Usernames.find(repository, "Kader ")).contains(kader);
    }

    @Test
    void le_nettoyage_au_demarrage_ne_vole_jamais_le_pseudo_d_un_autre() {
        User spaced = user("u1", "Spidey ");
        User doubled = user("u2", "Jean  Kader");
        User clash = user("u3", "kader ");
        User owner = user("u4", "Kader");
        when(repository.findAll()).thenReturn(List.of(spaced, doubled, clash, owner));

        new UsernameCleanup(repository).cleanUp();

        assertThat(spaced.getUsername()).isEqualTo("Spidey");
        assertThat(doubled.getUsername()).isEqualTo("Jean Kader");
        assertThat(clash.getUsername()).isEqualTo("kader ");
        assertThat(owner.getUsername()).isEqualTo("Kader");
        verify(repository).save(spaced);
        verify(repository).save(doubled);
        verify(repository, never()).save(clash);
    }
}
