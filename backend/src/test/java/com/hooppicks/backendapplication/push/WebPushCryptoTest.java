package com.hooppicks.backendapplication.push;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushCryptoTest {

    private static byte[] b64(String value) {
        return Base64.getUrlDecoder().decode(value.replaceAll("\\s", ""));
    }

    /** Vecteur de l'annexe A de la RFC 8291 : clé éphémère et sel imposés, résultat attendu à l'octet près. */
    @Test
    void reproduit_le_vecteur_de_test_de_la_rfc_8291() throws Exception {
        byte[] asPublic = b64("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8");
        byte[] asPrivate = b64("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw");
        byte[] uaPublic = b64("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] authSecret = b64("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = b64("DGv6ra1nlYgDCS1FRnbzlw");
        byte[] plaintext = "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.US_ASCII);

        KeyPair asKeys = new KeyPair(WebPushCrypto.publicKeyFromRaw(asPublic), WebPushCrypto.privateKeyFromRaw(asPrivate));

        byte[] body = WebPushCrypto.encrypt(plaintext, uaPublic, authSecret, asKeys, salt);

        // L'annexe donne l'en-tête (86 octets) et le chiffré séparément ; 86
        // n'étant pas multiple de 3, on décode chaque partie à part.
        byte[] header = b64("""
                DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27ml
                mlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8""");
        byte[] ciphertext = b64("""
                8pfeW0KbunFT06SuDKoJH9Ql87S1QUrdirN6GcG7sFz1y1sqLgVi1VhjVkHsUoEs
                bI_0LpXMuGvnzQ""");
        byte[] expected = new byte[header.length + ciphertext.length];
        System.arraycopy(header, 0, expected, 0, header.length);
        System.arraycopy(ciphertext, 0, expected, header.length, ciphertext.length);

        assertThat(header).hasSize(86);
        assertThat(body).isEqualTo(expected);
    }

    @Test
    void la_cle_publique_brute_fait_l_aller_retour() throws Exception {
        byte[] raw = b64("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        assertThat(WebPushCrypto.rawPublicKey(WebPushCrypto.publicKeyFromRaw(raw))).isEqualTo(raw);
    }
}
