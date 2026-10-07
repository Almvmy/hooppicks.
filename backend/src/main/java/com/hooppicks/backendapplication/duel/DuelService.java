package com.hooppicks.backendapplication.duel;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.DuelRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Duels 1 contre 1 sur la semaine de jeu en cours : un défi, une réponse,
 * puis le joueur qui a le plus de points de classement lundi 12h gagne.
 * Les points sont exactement ceux du classement de la semaine (même règle,
 * même requête), jamais un calcul à part qui pourrait diverger.
 */
@Service
public class DuelService {

    private static final Logger log = LoggerFactory.getLogger(DuelService.class);

    // Garde-fou anti-spam : défis en attente envoyés par un joueur sur une semaine.
    static final int MAX_PENDING_SENT = 10;
    // Clôture forcée même si un ticket reste en attente (synchro en panne) :
    // un duel ne doit pas rester ouvert indéfiniment.
    private static final Duration SETTLE_GRACE = Duration.ofHours(24);
    private static final Set<DuelStatus> BLOCKING = Set.of(DuelStatus.PENDING, DuelStatus.ACCEPTED, DuelStatus.FINISHED);

    private final DuelRepository duelRepository;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final Clock clock;

    @Autowired
    public DuelService(DuelRepository duelRepository, UserRepository userRepository, BetRepository betRepository,
                       NotificationRepository notificationRepository, PushService pushService) {
        this(duelRepository, userRepository, betRepository, notificationRepository, pushService,
                Clock.system(BankrollService.ZONE));
    }

    DuelService(DuelRepository duelRepository, UserRepository userRepository, BetRepository betRepository,
                NotificationRepository notificationRepository, PushService pushService, Clock clock) {
        this.duelRepository = duelRepository;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.clock = clock;
    }

    /** Erreur métier lisible, renvoyée telle quelle au joueur (400). */
    public static class DuelException extends RuntimeException {
        public DuelException(String message) {
            super(message);
        }
    }

    private LocalDate currentWeek() {
        return BankrollService.weekOf(clock.instant());
    }

    @Transactional
    public Duel challenge(String challengerId, String opponentUsername) {
        User challenger = userRepository.findById(challengerId).orElseThrow(() -> new DuelException("Compte introuvable."));
        User opponent = userRepository.findByUsername(opponentUsername)
                .orElseThrow(() -> new DuelException("Ce joueur n'existe pas."));
        if (opponent.getId().equals(challengerId)) throw new DuelException("Tu ne peux pas te défier toi-même.");

        LocalDate week = currentWeek();
        if (duelRepository.existsBetween(challengerId, opponent.getId(), week, BLOCKING)) {
            throw new DuelException("Un duel existe déjà entre vous cette semaine.");
        }
        if (duelRepository.countByChallengerIdAndStatusAndWeek(challengerId, DuelStatus.PENDING, week) >= MAX_PENDING_SENT) {
            throw new DuelException("Tu as déjà " + MAX_PENDING_SENT + " défis en attente de réponse cette semaine.");
        }

        Duel duel = new Duel();
        duel.setChallenger(challenger);
        duel.setOpponent(opponent);
        duel.setWeek(week);
        duelRepository.save(duel);

        notify(opponent, challenger.getUsername() + " te défie en duel cette semaine : le plus de points lundi 12h gagne.",
                "Nouveau duel", true);
        return duel;
    }

    @Transactional
    public Duel respond(String userId, String duelId, boolean accept) {
        Duel duel = find(duelId);
        if (!duel.getOpponent().getId().equals(userId)) throw new DuelException("Ce défi ne t'est pas adressé.");
        if (duel.getStatus() != DuelStatus.PENDING) throw new DuelException("Ce défi n'attend plus de réponse.");
        if (!duel.getWeek().equals(currentWeek())) throw new DuelException("Ce défi a expiré avec la semaine.");

        duel.setStatus(accept ? DuelStatus.ACCEPTED : DuelStatus.DECLINED);
        duel.setRespondedAt(clock.instant());
        // Refus : cloche seulement, pas de push pour une mauvaise nouvelle.
        notify(duel.getChallenger(), duel.getOpponent().getUsername()
                + (accept ? " a accepté ton duel. Que le meilleur gagne !" : " a décliné ton duel."), "Duel accepté", accept);
        return duel;
    }

    @Transactional
    public Duel cancel(String userId, String duelId) {
        Duel duel = find(duelId);
        if (!duel.getChallenger().getId().equals(userId)) throw new DuelException("Seul celui qui a lancé le défi peut l'annuler.");
        if (duel.getStatus() != DuelStatus.PENDING) throw new DuelException("Un duel accepté ne s'annule plus.");
        duel.setStatus(DuelStatus.CANCELLED);
        return duel;
    }

    private Duel find(String duelId) {
        return duelRepository.findById(duelId).orElseThrow(() -> new DuelException("Duel introuvable."));
    }

    /** Duels du joueur, avec les points en direct de ceux qui se jouent cette semaine. */
    @Transactional(readOnly = true)
    public List<DuelDto> list(String userId) {
        List<Duel> duels = duelRepository.findAllForUser(userId);
        LocalDate week = currentWeek();
        List<String> liveIds = duels.stream()
                .filter(d -> d.getStatus() == DuelStatus.ACCEPTED && d.getWeek().equals(week))
                .flatMap(d -> java.util.stream.Stream.of(d.getChallenger().getId(), d.getOpponent().getId()))
                .distinct().toList();
        Map<String, Long> live = liveIds.isEmpty() ? Map.of() : weekScores(week, liveIds);
        return duels.stream().map(d -> DuelDto.from(d, userId, live, week)).toList();
    }

    @Transactional(readOnly = true)
    public long winsOf(String userId) {
        return duelRepository.countWins(userId);
    }

    private Map<String, Long> weekScores(LocalDate week, List<String> userIds) {
        Map<String, Long> scores = new HashMap<>();
        for (Object[] row : betRepository.getWeekScores(week.atStartOfDay(), userIds)) {
            scores.put((String) row[0], ((Number) row[1]).longValue());
        }
        return scores;
    }

    /**
     * Clôt les duels des semaines terminées : points figés, vainqueur,
     * notification aux deux joueurs. On attend que leurs tickets de la
     * semaine soient tous réglés (24 h au plus), sinon un dernier match
     * pas encore synchronisé changerait le vainqueur après coup. Les défis
     * restés sans réponse expirent. Renvoie le nombre de duels clos.
     */
    @Transactional
    public int settleEndedWeeks() {
        LocalDate week = currentWeek();
        for (Duel d : duelRepository.findByStatusAndWeekBefore(DuelStatus.PENDING, week)) {
            d.setStatus(DuelStatus.EXPIRED);
        }

        int settled = 0;
        Instant now = clock.instant();
        for (Duel d : duelRepository.findByStatusAndWeekBefore(DuelStatus.ACCEPTED, week)) {
            List<String> ids = List.of(d.getChallenger().getId(), d.getOpponent().getId());
            Instant start = BankrollService.startOf(d.getWeek());
            Instant end = start.plus(Duration.ofDays(7));
            boolean pending = betRepository.countPendingPlacedBetween(ids, start, end) > 0;
            if (pending && now.isBefore(end.plus(SETTLE_GRACE))) continue;

            Map<String, Long> scores = weekScores(d.getWeek(), ids);
            long c = scores.getOrDefault(ids.get(0), 0L);
            long o = scores.getOrDefault(ids.get(1), 0L);
            d.setChallengerPoints(c);
            d.setOpponentPoints(o);
            d.setWinnerId(c == o ? null : (c > o ? ids.get(0) : ids.get(1)));
            d.setStatus(DuelStatus.FINISHED);
            notifyResult(d, d.getChallenger(), d.getOpponent(), c, o);
            notifyResult(d, d.getOpponent(), d.getChallenger(), o, c);
            settled++;
        }
        if (settled > 0) log.info("Duels : {} duel(s) clos", settled);
        return settled;
    }

    private void notifyResult(Duel d, User me, User other, long mine, long theirs) {
        String outcome = mine == theirs ? "Égalité" : mine > theirs ? "Victoire" : "Défaite";
        notify(me, outcome + " dans ton duel contre " + other.getUsername() + " : " + mine + " pts à " + theirs + ".",
                "Duel terminé", true);
    }

    // Même préférence que l'activité des ligues : c'est de l'activité entre joueurs.
    private void notify(User user, String message, String pushTitle, boolean push) {
        AppNotification n = new AppNotification();
        n.setUser(user);
        n.setType(NotificationType.DUEL);
        n.setMessage(message);
        notificationRepository.save(n);
        if (push && user.isNotifyLeagueActivity()) {
            pushService.sendToUser(user.getId(), new PushService.PushMessage(pushTitle, message, "/duels"));
        }
    }
}
