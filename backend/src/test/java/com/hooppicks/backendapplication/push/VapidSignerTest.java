package com.hooppicks.backendapplication.push;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class VapidSignerTest {

    @Test
    void produit_un_jwt_es256_verifiable_avec_la_cle_publique() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keys = generator.generateKeyPair();
        String publicKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(WebPushCrypto.rawPublicKey((ECPublicKey) keys.getPublic()));

        String header = VapidSigner.authorizationHeader(
                "https://fcm.googleapis.com/fcm/send/abc:def", "mailto:test@exemple.com",
                publicKey, (ECPrivateKey) keys.getPrivate());

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + publicKey);
        String jwt = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);

        String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        // aud = origine du service push uniquement, sans le chemin de l'abonnement.
        assertThat(claims).contains("\"aud\":\"https://fcm.googleapis.com\"").contains("\"sub\":\"mailto:test@exemple.com\"");

        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
        assertThat(signature).hasSize(64); // R || S, format JWT (pas du DER)
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(keys.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(signature)).isTrue();
    }
}
