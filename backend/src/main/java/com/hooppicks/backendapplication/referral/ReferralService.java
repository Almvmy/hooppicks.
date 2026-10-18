package com.hooppicks.backendapplication.referral;

import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Parrainage : un lien d'invitation (/invite/pseudo) mène à l'inscription,
 * puis le nouveau compte déclare son parrain. Une seule fois, juste après
 * l'inscription (un compte ancien ne peut pas se rattacher à quelqu'un après
 * coup), jamais soi-même. Ne rapporte qu'un badge : pas de points en jeu.
 */
@Service
public class ReferralService {

    public static final int RECRUITER_GOAL = 3;
    static final Duration CLAIM_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final Clock clock;

    @Autowired
    public ReferralService(UserRepository userRepository, NotificationRepository notificationRepository, PushService pushService) {
        this(userRepository, notificationRepository, pushService, Clock.systemUTC());
    }

    ReferralService(UserRepository userRepository, NotificationRepository notificationRepository, PushService pushService, Clock clock) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.clock = clock;
    }

    /** Rattache le compte à son parrain ; false si rien n'a été fait (lien périmé, déjà parrainé…). */
    @Transactional
    public boolean claim(String userId, String referrerUsername) {
        if (referrerUsername == null || referrerUsername.isBlank()) return false;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getReferredBy() != null) return false;
        Instant created = user.getCreatedAt();
        if (created == null || created.isBefore(clock.instant().minus(CLAIM_WINDOW))) return false;
        User referrer = userRepository.findByUsernameIgnoreCase(referrerUsername.trim()).orElse(null);
        if (referrer == null || referrer.getId().equals(userId)) return false;

        user.setReferredBy(referrer.getId());
        userRepository.save(user);

        long count = userRepository.countByReferredBy(referrer.getId());
        String message = "@" + user.getUsername() + " a rejoint HoopPicks grâce à ton invitation"
                + (count < RECRUITER_GOAL ? " (" + count + "/" + RECRUITER_GOAL + " pour le badge Recruteur)."
                : count == RECRUITER_GOAL ? " : badge Recruteur débloqué !" : ".");
        AppNotification n = new AppNotification();
        n.setUser(referrer);
        n.setType(NotificationType.REFERRAL);
        n.setMessage(message);
        notificationRepository.save(n);
        pushService.sendToUser(referrer.getId(), new PushService.PushMessage("Nouvelle recrue", message, "/profile"));
        return true;
    }

    public long recruits(String userId) {
        return userRepository.countByReferredBy(userId);
    }
}
