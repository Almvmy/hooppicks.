package com.hooppicks.backendapplication.push;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/push")
public class PushController {

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SessionStore sessionStore;

    public PushController(PushService pushService, PushSubscriptionRepository subscriptionRepository,
                          UserRepository userRepository, SessionStore sessionStore) {
        this.pushService = pushService;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.sessionStore = sessionStore;
    }

    public record SubscriptionKeys(String p256dh, String auth) {}
    public record SubscribeRequest(String endpoint, SubscriptionKeys keys) {}
    public record UnsubscribeRequest(String endpoint) {}

    /** Clé publique VAPID dont le navigateur a besoin pour s'abonner. 404 si le push n'est pas configuré. */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> publicKey() {
        if (!pushService.isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("publicKey", pushService.getPublicKey()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody SubscribeRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        if (!pushService.isEnabled()) return ResponseEntity.notFound().build();
        if (!isValid(body)) return ResponseEntity.badRequest().build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        // Upsert par endpoint : un même navigateur qui se réabonne (ou change
        // de compte sur le même appareil) met à jour sa ligne au lieu d'en
        // créer une deuxième, qui recevrait chaque notification en double.
        PushSubscription subscription = subscriptionRepository.findByEndpoint(body.endpoint())
                .orElseGet(PushSubscription::new);
        boolean isNew = subscription.getId() == null;
        subscription.setUser(user);
        subscription.setEndpoint(body.endpoint());
        subscription.setP256dh(body.keys().p256dh());
        subscription.setAuth(body.keys().auth());
        subscriptionRepository.save(subscription);

        // Confirmation immédiate sur l'appareil qui vient de s'abonner : la
        // personne voit tout de suite que ça marche (et nous aussi : c'est le
        // test de bout en bout le plus simple de toute la chaîne push).
        if (isNew) {
            pushService.sendToUser(userId, new PushService.PushMessage(
                    "Notifications activées",
                    "Tu recevras ici tes résultats de paris et les coups d'envoi de tes matchs.",
                    "/settings"));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody UnsubscribeRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        if (body == null || body.endpoint() == null) return ResponseEntity.badRequest().build();
        // Limité aux abonnements de l'utilisateur connecté : impossible de
        // désabonner l'appareil de quelqu'un d'autre en devinant son endpoint.
        subscriptionRepository.deleteByEndpointAndUserId(body.endpoint(), userId);
        return ResponseEntity.noContent().build();
    }

    // Services push des navigateurs (Chrome/Edge/Android, Firefox, Safari/iOS,
    // Windows). C'est le backend qui appellera l'endpoint : on n'accepte que
    // ces domaines, sinon un client pourrait lui faire envoyer des requêtes
    // vers n'importe quelle adresse (y compris un réseau interne).
    private static final java.util.List<String> PUSH_SERVICE_DOMAINS = java.util.List.of(
            "fcm.googleapis.com", "push.services.mozilla.com", "push.apple.com", "notify.windows.com");

    static boolean isValid(SubscribeRequest body) {
        if (body == null || body.endpoint() == null || body.keys() == null
                || body.keys().p256dh() == null || body.keys().auth() == null
                || body.endpoint().length() > 1024) {
            return false;
        }
        try {
            URI uri = URI.create(body.endpoint());
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && host != null
                    && PUSH_SERVICE_DOMAINS.stream().anyMatch(d -> host.equals(d) || host.endsWith("." + d));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
