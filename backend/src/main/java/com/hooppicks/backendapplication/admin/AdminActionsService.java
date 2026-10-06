package com.hooppicks.backendapplication.admin;

import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.TransactionType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.entity.WalletTransaction;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Supplier;

/**
 * Actions de la console admin qui touchent aux points des joueurs (annuler
 * un pari, ajuster un solde) ou les contactent (annonce). Toutes exigent un
 * motif, sont tracées dans le journal d'audit et préviennent le joueur.
 *
 * Les actions sur les points passent sous le verrou de la synchro
 * (NbaSyncService#tryRunExclusive), comme la résolution des paris : sans
 * ça, annuler un pari pendant que la résolution le règle le paierait deux
 * fois (remboursement + gain). Si une synchro tourne, l'action est refusée
 * (409) plutôt que d'attendre : l'admin réessaie dans quelques secondes.
 */
@Service
public class AdminActionsService {

    static final int MAX_ADJUSTMENT = 100_000;
    static final int MAX_ANNOUNCEMENT_LENGTH = 280;

    private final BetRepository betRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final NbaSyncService nbaSyncService;
    private final AdminAuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public AdminActionsService(BetRepository betRepository, MatchRepository matchRepository,
                               UserRepository userRepository, WalletTransactionRepository transactionRepository,
                               NotificationRepository notificationRepository, PushService pushService,
                               NbaSyncService nbaSyncService, AdminAuditService auditService,
                               PlatformTransactionManager transactionManager) {
        this.betRepository = betRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.nbaSyncService = nbaSyncService;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Annule un pari en attente et rembourse sa mise. */
    public void voidBet(String betId, String reason, User admin) {
        String motive = requireReason(reason);
        exclusive(() -> {
            Bet bet = betRepository.findById(betId)
                    .orElseThrow(() -> new AdminActionException(404, "Pari introuvable."));
            if (bet.getStatus() != BetStatus.PENDING) {
                throw new AdminActionException(400, "Seul un pari en attente peut être annulé.");
            }
            refund(bet, motive);
            auditService.log(admin, "VOID_BET", bet.getUser().getUsername(),
                    bet.getStake() + " pts remboursés · " + motive);
            return null;
        });
    }

    /** Annule et rembourse tous les paris en attente qui portent sur ce match (match reporté, annulé…). */
    public int voidPendingBetsForMatch(String matchId, String reason, User admin) {
        String motive = requireReason(reason);
        return exclusive(() -> {
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new AdminActionException(404, "Match introuvable."));
            List<Bet> bets = betRepository.findPendingBetsForMatch(matchId);
            for (Bet bet : bets) refund(bet, motive);
            auditService.log(admin, "VOID_MATCH_BETS", matchLabel(match),
                    bets.size() + " pari(s) remboursé(s) · " + motive);
            return bets.size();
        });
    }

    /** Crédite (montant positif) ou débite (négatif) le solde d'un joueur. Renvoie le nouveau solde. */
    public int adjustWallet(String userId, int amount, String reason, User admin) {
        String motive = requireReason(reason);
        if (amount == 0 || Math.abs(amount) > MAX_ADJUSTMENT) {
            throw new AdminActionException(400, "Montant invalide (entre 1 et " + MAX_ADJUSTMENT + " pts, positif ou négatif).");
        }
        return exclusive(() -> {
            // Même verrou de ligne que placeBet : pas de solde écrasé par un pari posé au même instant.
            User user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new AdminActionException(404, "Utilisateur introuvable."));
            int newBalance = user.getWalletBalance() + amount;
            if (newBalance < 0) {
                throw new AdminActionException(400, "Le solde deviendrait négatif (solde actuel : " + user.getWalletBalance() + " pts).");
            }
            user.setWalletBalance(newBalance);
            userRepository.save(user);

            WalletTransaction tx = new WalletTransaction();
            tx.setUser(user);
            tx.setType(TransactionType.BONUS);
            tx.setAmount(amount);
            tx.setDescription("Ajustement par l'équipe HoopPicks : " + motive);
            transactionRepository.save(tx);

            String signed = (amount > 0 ? "+" : "") + amount;
            notify(user, "Solde ajusté", "Ton solde a été ajusté de " + signed + " pts : " + motive, "/profile");
            auditService.log(admin, "ADJUST_WALLET", user.getUsername(), signed + " pts · " + motive);
            return newBalance;
        });
    }

    /** Annonce à tous les joueurs (cloche, et push si demandé). Renvoie le nombre de destinataires. */
    public int announce(String message, boolean push, User admin) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty() || text.length() > MAX_ANNOUNCEMENT_LENGTH) {
            throw new AdminActionException(400, "L'annonce doit faire entre 1 et " + MAX_ANNOUNCEMENT_LENGTH + " caractères.");
        }
        Integer count = transactionTemplate.execute(status -> {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                AppNotification n = new AppNotification();
                n.setUser(user);
                n.setType(NotificationType.SYSTEM);
                n.setMessage(text);
                notificationRepository.save(n);
                if (push) pushService.sendToUser(user.getId(), new PushService.PushMessage("HoopPicks", text, "/dashboard"));
            }
            auditService.log(admin, "ANNOUNCE", users.size() + " joueur(s)", (push ? "[push] " : "") + text);
            return users.size();
        });
        return count == null ? 0 : count;
    }

    private void refund(Bet bet, String motive) {
        User user = userRepository.findByIdForUpdate(bet.getUser().getId())
                .orElseThrow(() -> new AdminActionException(404, "Utilisateur introuvable."));
        bet.setStatus(BetStatus.VOID);
        bet.setResolvedAt(java.time.Instant.now());
        betRepository.save(bet);

        user.setWalletBalance(user.getWalletBalance() + bet.getStake());
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(TransactionType.BONUS);
        tx.setAmount(bet.getStake());
        tx.setDescription("Remboursement (pari annulé par l'équipe HoopPicks : " + motive + ")");
        transactionRepository.save(tx);

        // Toujours prévenu, quelle que soit sa préférence « résultats de
        // paris » : c'est une décision prise sur ses points, pas un résultat.
        notify(user, "Pari annulé", "Ton pari de " + bet.getStake() + " pts a été annulé et remboursé : " + motive, "/bets");
    }

    private void notify(User user, String title, String message, String url) {
        AppNotification n = new AppNotification();
        n.setUser(user);
        n.setType(NotificationType.SYSTEM);
        n.setMessage(message);
        notificationRepository.save(n);
        pushService.sendToUser(user.getId(), new PushService.PushMessage(title, message, url));
    }

    private <T> T exclusive(Supplier<T> work) {
        Object[] result = new Object[1];
        boolean ran = nbaSyncService.tryRunExclusive(() -> result[0] = transactionTemplate.execute(status -> work.get()));
        if (!ran) {
            throw new AdminActionException(409, "Une synchro est en cours : réessaie dans quelques secondes.");
        }
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
    }

    private static String requireReason(String reason) {
        String motive = reason == null ? "" : reason.trim();
        if (motive.length() < 3) throw new AdminActionException(400, "Un motif est obligatoire.");
        if (motive.length() > 200) throw new AdminActionException(400, "Motif trop long (200 caractères max).");
        return motive;
    }

    static String matchLabel(Match match) {
        return match.getAwayTeam().getAbbreviation() + " @ " + match.getHomeTeam().getAbbreviation();
    }
}
