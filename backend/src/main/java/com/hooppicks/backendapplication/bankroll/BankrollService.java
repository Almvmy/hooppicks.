package com.hooppicks.backendapplication.bankroll;

import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.TransactionType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.entity.WalletTransaction;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Semaines de jeu : chaque lundi à midi (GMT), tout le monde
 * repart avec le même solde de pari. Le solde ne sert qu'à jouer ; le
 * classement compte le bénéfice net des paris (cf. BetRepository), attribué
 * à la semaine où chaque pari a été posé.
 *
 * Midi plutôt que minuit : les matchs NBA se jouent entre minuit et 6h30
 * GMT environ, à midi ceux de la nuit sont terminés et les suivants
 * pas commencés.
 *
 * Remise à zéro plutôt qu'un versement qui s'ajoute : sinon les soldes
 * divergent semaine après semaine et les joueurs ne jouent plus avec les
 * mêmes moyens. Un joueur à 0 rejoue au plus tard le lundi suivant.
 */
@Service
public class BankrollService {

    public static final int WEEKLY_BANKROLL = 1000;
    // Fuseau du jeu : GMT, celui des joueurs (Côte d'Ivoire), sans heure
    // d'été. Partagé par le classement et la console admin.
    public static final ZoneId ZONE = ZoneId.of("GMT");
    public static final LocalTime RESET_TIME = LocalTime.NOON;

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final WalletTransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Autowired
    public BankrollService(UserRepository userRepository, BetRepository betRepository,
                           WalletTransactionRepository transactionRepository,
                           NotificationRepository notificationRepository) {
        this(userRepository, betRepository, transactionRepository, notificationRepository, Clock.system(ZONE));
    }

    BankrollService(UserRepository userRepository, BetRepository betRepository,
                    WalletTransactionRepository transactionRepository,
                    NotificationRepository notificationRepository, Clock clock) {
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    /** Début de la semaine de jeu qui contient cet instant (lundi 12h GMT). */
    public static ZonedDateTime weekStart(Instant at) {
        ZonedDateTime t = at.atZone(ZONE);
        ZonedDateTime start = t.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atTime(RESET_TIME).atZone(ZONE);
        // Lundi matin : on est encore dans la semaine de jeu précédente.
        return t.isBefore(start) ? start.minusWeeks(1) : start;
    }

    /** Identifiant d'une semaine de jeu : la date de son lundi. */
    public static LocalDate weekOf(Instant at) {
        return weekStart(at).toLocalDate();
    }

    private static Instant startOf(LocalDate week) {
        return week.atTime(RESET_TIME).atZone(ZONE).toInstant();
    }

    public LocalDate currentWeek() {
        return weekOf(clock.instant());
    }

    public Instant currentWeekStart() {
        return weekStart(clock.instant()).toInstant();
    }

    /**
     * Le gain ou le remboursement de ce pari va-t-il sur le solde actuel du
     * joueur ? Seulement s'il a été posé pendant la semaine de ce solde : un
     * pari de la semaine passée compte à son classement, mais ne doit pas
     * gonfler le solde neuf avec lequel tout le monde repart.
     * À appeler après {@link #ensureCurrent(User)}.
     */
    public boolean paysIntoCurrentBalance(User user, Bet bet) {
        return bet.getPlacedAt() != null && weekOf(bet.getPlacedAt()).equals(user.getBankrollWeek());
    }

    /**
     * Fait passer le joueur à la semaine de jeu en cours si ce n'est pas déjà
     * fait. L'appelant doit tenir la ligne du joueur (findByIdForUpdate ou
     * verrou de la synchro) : sinon un pari posé au même instant serait écrasé.
     * Renvoie true si le solde a été remis à niveau.
     */
    public boolean ensureCurrent(User user) {
        LocalDate current = currentWeek();
        LocalDate previous = user.getBankrollWeek();
        if (current.equals(previous)) return false;

        // Normalement 0. Seuls cas non nuls : un pari posé lundi après midi
        // juste avant ce passage, et la mise en place des semaines de jeu en
        // cours de semaine. Sa mise est déjà engagée, on ne la rend pas.
        long pending = betRepository.sumPendingStakesSince(user.getId(), startOf(current));
        int newBalance = (int) Math.max(0, WEEKLY_BANKROLL - pending);
        int delta = newBalance - user.getWalletBalance();

        user.setWalletBalance(newBalance);
        user.setBankrollWeek(current);
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(TransactionType.WEEKLY_BANKROLL);
        tx.setAmount(delta);
        tx.setDescription("Nouvelle semaine de jeu : solde remis à " + WEEKLY_BANKROLL + " pts");
        transactionRepository.save(tx);

        notifyNewWeek(user, previous);
        return true;
    }

    /** Fait passer tous les joueurs en retard à la semaine en cours. Renvoie le nombre de joueurs traités. */
    @Transactional
    public int rollOverAll() {
        LocalDate current = currentWeek();
        int count = 0;
        for (String id : userRepository.findIdsWithBankrollWeekNot(current)) {
            User user = userRepository.findByIdForUpdate(id).orElse(null);
            if (user != null && ensureCurrent(user)) count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public boolean rollOverNeeded() {
        return !userRepository.findIdsWithBankrollWeekNot(currentWeek()).isEmpty();
    }

    // Cloche uniquement, pas de push : un message par semaine à tous les
    // joueurs actifs serait vite pris pour du spam sur le téléphone.
    private void notifyNewWeek(User user, LocalDate previous) {
        String message;
        if (previous == null) {
            message = "Nouveau : chaque lundi à 12h, tout le monde repart à " + WEEKLY_BANKROLL
                    + " pts. Le classement compte ton bénéfice net (gains moins mises). Ton solde vient d'être remis à "
                    + user.getWalletBalance() + " pts.";
        } else {
            List<Object[]> rows = betRepository.getNetResultBetween(user.getId(), startOf(previous), startOf(previous.plusWeeks(1)));
            Object[] row = rows.isEmpty() ? null : rows.get(0);
            long tickets = row == null || row[0] == null ? 0 : ((Number) row[0]).longValue();
            if (tickets == 0) return;
            long net = row[1] == null ? 0 : ((Number) row[1]).longValue();
            message = "Semaine terminée : " + (net > 0 ? "+" : "") + net + " pts au classement sur "
                    + tickets + " ticket(s). Ton solde repart à " + user.getWalletBalance() + " pts.";
        }
        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setType(NotificationType.SYSTEM);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }
}
