package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.bet.LegEvaluator.LegResult;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BetResolutionService {

    private final BetRepository betRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final PushService pushService;
    private final BankrollService bankrollService;

    public BetResolutionService(BetRepository betRepository, MatchRepository matchRepository,
                                UserRepository userRepository, WalletTransactionRepository transactionRepository,
                                NotificationRepository notificationRepository, PushService pushService,
                                BankrollService bankrollService) {
        this.betRepository = betRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.pushService = pushService;
        this.bankrollService = bankrollService;
    }


    @Transactional
    public int resolvePendingBets() {
        List<Bet> pendingBets = betRepository.findByStatus(BetStatus.PENDING);
        int resolvedCount = 0;

        for (Bet bet : pendingBets) {
            List<String> matchIds = bet.getSelections().stream().map(BetSelection::getMatchId).toList();
            Map<String, Match> matchesById = matchRepository.findAllById(matchIds).stream()
                    .collect(Collectors.toMap(Match::getId, Function.identity()));

            // On ne résout un ticket que si TOUS ses matchs sont terminés
            boolean allFinished = matchIds.stream()
                    .allMatch(id -> matchesById.containsKey(id) && matchesById.get(id).getStatus() == MatchStatus.FINISHED);
            if (!allFinished) continue;

            boolean anyLoss = false;
            boolean allPush = true;
            boolean anyPush = false;
            // Produit des cotes des seules sélections gagnantes : une sélection
            // remboursée (égalité pile sur la ligne) compte pour une cote de 1,00,
            // comme chez n'importe quel bookmaker. Avant, elle gardait sa cote
            // et le combiné était payé comme si elle avait gagné.
            double winningOdds = 1.0;

            for (BetSelection selection : bet.getSelections()) {
                Match match = matchesById.get(selection.getMatchId());
                LegResult result = LegEvaluator.evaluate(selection, match);
                if (result == LegResult.LOSE) anyLoss = true;
                if (result != LegResult.PUSH) allPush = false;
                if (result == LegResult.PUSH) anyPush = true;
                if (result == LegResult.WIN) winningOdds *= selection.getOdds();
            }

            User user = bet.getUser();
            bet.setResolvedAt(java.time.Instant.now());
            // Le joueur passe d'abord à la semaine de jeu en cours s'il ne l'a
            // pas encore fait : on sait alors si ce pari appartient à son solde actuel.
            bankrollService.ensureCurrent(user);
            boolean credited = bankrollService.paysIntoCurrentBalance(user, bet);

            if (anyLoss) {
                bet.setStatus(BetStatus.LOST);
                logTransaction(user, TransactionType.BET_LOSS, 0,
                        "Pari perdu (" + bet.getSelections().size() + " sélection(s))");
                notify(user, NotificationType.BET_LOST, "Ticket perdu",
                        "Ticket perdu · " + describe(bet) + " : -" + bet.getStake() + " pts au classement.");
            } else if (allPush) {
                // Aucune sélection perdue, mais aucune vraiment gagnée non plus (égalité pile sur le seuil) : on rembourse la mise
                bet.setStatus(BetStatus.VOID);
                if (credited) {
                    user.setWalletBalance(user.getWalletBalance() + bet.getStake());
                    userRepository.save(user);
                    logTransaction(user, TransactionType.BONUS, bet.getStake(), "Remboursement (pari annulé, égalité sur le seuil)");
                    notify(user, NotificationType.SYSTEM, "Ticket remboursé",
                            "Ticket remboursé · " + describe(bet) + " : égalité pile sur la ligne, tes " + bet.getStake()
                                    + " pts te sont rendus. Rien ne change au classement.");
                } else {
                    notify(user, NotificationType.SYSTEM, "Ticket annulé",
                            "Ticket de la semaine passée annulé · " + describe(bet) + " : égalité pile sur la ligne, il ne compte pas au classement.");
                }
            } else {
                if (anyPush) {
                    // Gain recalculé sans les sélections remboursées. Le ticket est
                    // mis à jour pour que "Mes paris" affiche ce qui a réellement
                    // été crédité (mise × cote = gain reste cohérent à l'écran).
                    bet.setTotalOdds(Math.round(winningOdds * 100) / 100.0);
                    bet.setPotentialPayout((int) Math.round(bet.getStake() * winningOdds));
                }
                bet.setStatus(BetStatus.WON);
                if (credited) {
                    user.setWalletBalance(user.getWalletBalance() + bet.getPotentialPayout());
                    userRepository.save(user);
                    logTransaction(user, TransactionType.BET_WIN, bet.getPotentialPayout(),
                            "Pari gagné (+" + bet.getPotentialPayout() + " pts)");
                    // Deux chiffres distincts, chacun nommé : ce qui revient sur le
                    // solde (mise comprise) et ce qui compte au classement.
                    notify(user, NotificationType.BET_WON, "Ticket gagnant",
                            "Ticket gagnant · " + describe(bet) + " : +" + (bet.getPotentialPayout() - bet.getStake())
                                    + " pts au classement (" + bet.getPotentialPayout() + " pts versés sur ton solde, mise comprise).");
                } else {
                    // Pari de la semaine passée : le gain compte au classement de
                    // cette semaine-là, mais ne gonfle pas le solde neuf.
                    int net = bet.getPotentialPayout() - bet.getStake();
                    logTransaction(user, TransactionType.BET_WIN, 0,
                            "Pari gagné de la semaine passée (+" + net + " pts au classement, solde déjà remis à niveau)");
                    notify(user, NotificationType.BET_WON, "Ticket gagnant",
                            "Ticket de la semaine passée gagnant · " + describe(bet) + " : +" + net + " pts au classement de cette semaine-là.");
                }
            }

            betRepository.save(bet);
            resolvedCount++;
        }

        return resolvedCount;
    }

    /** « LAL (V) · Lakers vs Celtics » ou « combiné de 3 : LAL (V), BOS -4, Plus de 228.5 ». */
    static String describe(Bet bet) {
        List<BetSelection> legs = bet.getSelections();
        if (legs.size() == 1) {
            BetSelection s = legs.get(0);
            return s.getMatchLabel() == null ? s.getLabel() : s.getLabel() + " · " + s.getMatchLabel();
        }
        String shown = legs.stream().limit(3).map(BetSelection::getLabel).collect(Collectors.joining(", "));
        return "combiné de " + legs.size() + " : " + shown + (legs.size() > 3 ? "…" : "");
    }

    private void logTransaction(User user, TransactionType type, int amount, String description) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDescription(description);
        transactionRepository.save(tx);
    }

    private void notify(User user, NotificationType type, String title, String message) {
        if (!user.isNotifyBetResults()) return;

        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notificationRepository.save(notification);
        pushService.sendToUser(user.getId(), new PushService.PushMessage(title, message, "/bets"));
    }
}