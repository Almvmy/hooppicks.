package com.hooppicks.backendapplication.bet;

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

    public BetResolutionService(BetRepository betRepository, MatchRepository matchRepository,
                                UserRepository userRepository, WalletTransactionRepository transactionRepository,
                                NotificationRepository notificationRepository) {
        this.betRepository = betRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
    }

    private enum LegResult { WIN, LOSE, PUSH }

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

            for (BetSelection selection : bet.getSelections()) {
                Match match = matchesById.get(selection.getMatchId());
                LegResult result = evaluateSelection(selection, match);
                if (result == LegResult.LOSE) anyLoss = true;
                if (result != LegResult.PUSH) allPush = false;
            }

            User user = bet.getUser();
            bet.setResolvedAt(java.time.Instant.now());

            if (anyLoss) {
                bet.setStatus(BetStatus.LOST);
                logTransaction(user, TransactionType.BET_LOSS, 0,
                        "Pari perdu (" + bet.getSelections().size() + " sélection(s))");
                notify(user, NotificationType.BET_LOST, "Ton pari n'est pas gagnant.");
            } else if (allPush) {
                // Aucune sélection perdue, mais aucune vraiment gagnée non plus (égalité pile sur le seuil) : on rembourse la mise
                bet.setStatus(BetStatus.VOID);
                user.setWalletBalance(user.getWalletBalance() + bet.getStake());
                userRepository.save(user);
                logTransaction(user, TransactionType.BONUS, bet.getStake(), "Remboursement (pari annulé, égalité sur le seuil)");
                notify(user, NotificationType.SYSTEM, "Ton pari a été annulé (égalité sur le seuil) — mise remboursée : +" + bet.getStake() + " pts");
            } else {
                bet.setStatus(BetStatus.WON);
                user.setWalletBalance(user.getWalletBalance() + bet.getPotentialPayout());
                userRepository.save(user);
                logTransaction(user, TransactionType.BET_WIN, bet.getPotentialPayout(),
                        "Pari gagné (+" + bet.getPotentialPayout() + " pts)");
                notify(user, NotificationType.BET_WON, "Ton pari est gagnant : +" + bet.getPotentialPayout() + " pts");
            }

            betRepository.save(bet);
            resolvedCount++;
        }

        return resolvedCount;
    }

    private LegResult evaluateSelection(BetSelection selection, Match match) {
        int home = match.getHomeScore() != null ? match.getHomeScore() : 0;
        int away = match.getAwayScore() != null ? match.getAwayScore() : 0;
        boolean selectionIsHome = "home".equals(selection.getOutcome());

        return switch (selection.getMarket()) {
            case "moneyline" -> {
                if (home == away) yield LegResult.PUSH; // n'arrive jamais au basket (pas de match nul), gardé par sécurité
                boolean homeWins = home > away;
                yield (homeWins == selectionIsHome) ? LegResult.WIN : LegResult.LOSE;
            }
            case "spread" -> {
                double homeAdjusted = home + match.getSpreadValue();
                if (homeAdjusted == away) yield LegResult.PUSH;
                boolean homeCovers = homeAdjusted > away;
                yield (homeCovers == selectionIsHome) ? LegResult.WIN : LegResult.LOSE;
            }
            case "total" -> {
                int total = home + away;
                if (total == match.getTotalValue()) yield LegResult.PUSH;
                boolean overWins = total > match.getTotalValue();
                boolean selectionIsOver = "over".equals(selection.getOutcome());
                yield (overWins == selectionIsOver) ? LegResult.WIN : LegResult.LOSE;
            }
            default -> throw new IllegalStateException("Marché inconnu : " + selection.getMarket());
        };
    }

    private void logTransaction(User user, TransactionType type, int amount, String description) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDescription(description);
        transactionRepository.save(tx);
    }

    private void notify(User user, NotificationType type, String message) {
        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }
}