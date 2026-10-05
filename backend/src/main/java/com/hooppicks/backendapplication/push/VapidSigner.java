package com.hooppicks.backendapplication.push;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Base64;

/**
 * En-tête Authorization VAPID (RFC 8292) : un JWT ES256 signé avec la clé
 * privée du serveur, qui prouve au service push (Google, Mozilla, Apple)
 * que l'envoi vient bien de nous.
 */
final class VapidSigner {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private VapidSigner() {}

    static String authorizationHeader(String endpoint, String subject, String publicKey, ECPrivateKey privateKey)
            throws GeneralSecurityException {
        URI uri = URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        // 12 h : sous la limite de 24 h imposée par la RFC, avec de la marge
        // pour un décalage d'horloge.
        long expiresAt = Instant.now().plusSeconds(12 * 3600).getEpochSecond();

        String header = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = B64.encodeToString(("{\"aud\":\"" + audience + "\",\"exp\":" + expiresAt
                + ",\"sub\":\"" + subject + "\"}").getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + claims;

        // Format P1363 (R || S brut, 64 octets) : celui qu'exige un JWT ES256,
        // et non l'encodage DER que produit "SHA256withECDSA".
        Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
        signer.initSign(privateKey);
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String jwt = signingInput + "." + B64.encodeToString(signer.sign());

        return "vapid t=" + jwt + ", k=" + publicKey;
    }
}
