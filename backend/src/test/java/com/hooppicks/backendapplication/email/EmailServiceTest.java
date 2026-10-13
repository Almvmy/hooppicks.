package com.hooppicks.backendapplication.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceTest {

    @Test
    void nettoie_une_cle_collee_avec_espaces_retour_a_la_ligne_ou_guillemets() {
        assertThat(EmailService.cleanKey("  xkeysib-abc\n")).isEqualTo("xkeysib-abc");
        assertThat(EmailService.cleanKey("\"xkeysib-abc\"")).isEqualTo("xkeysib-abc");
        assertThat(EmailService.cleanKey("'xkeysib-abc'")).isEqualTo("xkeysib-abc");
        assertThat(EmailService.cleanKey(null)).isEmpty();
    }
}
