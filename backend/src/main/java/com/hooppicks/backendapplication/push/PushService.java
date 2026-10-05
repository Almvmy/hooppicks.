package com.hooppicks.backendapplication.push;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Envoi des notifications push (Web Push + VAPID) vers les appareils
 * abonnés. Désactivé proprement si les clés VAPID ne sont pas configurées,
 * comme Sentry : l'app fonctionne alors exactement comme avant (notifications
 * dans la cloche uniquement).
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final Base64.Decoder B64 = Base64.getUrlDecoder();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final PushSubscriptionRepository subscriptionRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final String publicKey;
    private final String subject;
    private final ECPrivateKey privateKey;

    public PushService(PushSubscriptionRepository subscriptionRepository,
                       @Value("${push.vapid.public-key:}") String publicKey,
                       @Value("${push.vapid.private-key:}") String privateKey,
                       @Value("${push.vapid.subject:}") String subject,
                       @Value("${app.mail-from:}") String mailFrom) {
        this.subscriptionRepository = subscriptionRepository;
        this.publicKey = publicKey.trim();
        this.subject = !subject.isBlank() ? subject : "mailto:" + mailFrom;
        this.privateKey = parsePrivateKey(privateKey.trim());
        if (!isEnabled()) {
            log.info("Notifications push désactivées : clés VAPID absentes (push.vapid.*)");
        }
    }

    private static ECPrivateKey parsePrivateKey(String raw) {
        if (raw.isBlank()) return null;
        try {
            return WebPushCrypto.privateKeyFromRaw(B64.decode(raw));
        } catch (Exception e) {
            log.error("Clé privée VAPID illisible : push désactivé", e);
            return null;
        }
    }

    public boolean isEnabled() {
        return privateKey != null && !publicKey.isBlank();
    }

    public String getPublicKey() {
        return publicKey;
    }

    public record PushMessage(String title, String body, String url) {}

    /**
     * Envoie à tous les appareils de l'utilisateur. Appelé depuis du code
     * transactionnel (résolution des paris, synchro) : l'envoi est différé
     * après le commit, pour ne jamais notifier un résultat qui serait
     * finalement annulé par un rollback.
     */
    public void sendToUser(String userId, PushMessage message) {
        if (!isEnabled()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNow(userId, message);
                }
            });
        } else {
            sendNow(userId, message);
        }
    }

    private void sendNow(String userId, PushMessage message) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) return;

        byte[] payload = JSON.writeValueAsString(Map.of(
                "title", message.title(),
                "body", message.body(),
                "url", message.url() == null ? "/dashboard" : message.url()
        )).getBytes(StandardCharsets.UTF_8);

        for (PushSubscription subscription : subscriptions) {
            try {
                byte[] body = WebPushCrypto.encrypt(payload, B64.decode(subscription.getP256dh()), B64.decode(subscription.getAuth()));
                HttpRequest request = HttpRequest.newBuilder(URI.create(subscription.getEndpoint()))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Encoding", "aes128gcm")
                        .header("Content-Type", "application/octet-stream")
                        // 24 h : un résultat de pari reste pertinent le lendemain,
                        // un "match qui commence" livré 2 jours après non.
                        .header("TTL", "86400")
                        .header("Authorization", VapidSigner.authorizationHeader(
                                subscription.getEndpoint(), subject, publicKey, privateKey))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                // Asynchrone : un service push lent ne doit jamais retenir le
                // thread de la synchro NBA qui a déclenché la notification.
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .whenComplete((response, error) -> handleResponse(subscription, response, error));
            } catch (Exception e) {
                log.warn("Préparation d'un push impossible pour l'abonnement {}", subscription.getId(), e);
            }
        }
    }

    private void handleResponse(PushSubscription subscription, HttpResponse<Void> response, Throwable error) {
        if (error != null) {
            log.warn("Envoi push échoué vers {} : {}", URI.create(subscription.getEndpoint()).getHost(), error.getMessage());
            return;
        }
        int status = response.statusCode();
        // 404/410 : l'abonnement n'existe plus côté navigateur (désinstallé,
        // permission retirée, expiré). On l'oublie au lieu de réessayer à vie.
        if (status == 404 || status == 410) {
            subscriptionRepository.deleteById(subscription.getId());
        } else if (status >= 400) {
            log.warn("Service push a répondu {} pour l'abonnement {}", status, subscription.getId());
            if (status == 401 || status == 403) {
                // Clés VAPID refusées : problème de configuration, pas d'un appareil.
                Sentry.captureMessage("Push refusé (" + status + ") : vérifier les clés VAPID");
            }
        }
    }
}
