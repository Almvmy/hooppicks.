package com.hooppicks.backendapplication.badge;

import com.hooppicks.backendapplication.dto.BadgeDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BadgeService {

    private static final int SHARPSHOOTER_MIN_BETS = 10;
    private static final int SHARPSHOOTER_MIN_WIN_RATE = 60;
    private static final int BIG_WIN_THRESHOLD = 500;
    private static final int GRINDER_STAKE_THRESHOLD = 2000;
    private static final int PARLAY_MIN_SELECTIONS = 3;

    /**
     * Calcule le catalogue de badges pour un utilisateur à partir de ses paris.
     * bets doit être trié du plus récent au plus ancien (comme le renvoie
     * BetRepository#findByUserIdOrderByPlacedAtDesc), car la série de victoires
     * dépend de cet ordre.
     */
    public List<BadgeDto> computeBadges(List<Bet> bets) {
        long totalPlaced = bets.size();
        long totalResolved = bets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .count();
        long wonCount = bets.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
        int winRate = totalResolved == 0 ? 0 : (int) Math.round((wonCount * 100.0) / totalResolved);

        int winStreak = computeWinStreak(bets);
        boolean hasParlay = bets.stream().anyMatch(b -> b.getSelections().size() >= PARLAY_MIN_SELECTIONS);
        boolean hasBigWin = bets.stream()
                .anyMatch(b -> b.getStatus() == BetStatus.WON && b.getPotentialPayout() >= BIG_WIN_THRESHOLD);
        int totalStaked = bets.stream().mapToInt(Bet::getStake).sum();

        List<BadgeDto> badges = new ArrayList<>();
        badges.add(new BadgeDto("first_bet", "Premier ticket",
                "Place ton tout premier pari.", totalPlaced >= 1, "ticket"));
        badges.add(new BadgeDto("ten_bets", "Habitué",
                "Place 10 paris au total.", totalPlaced >= 10, "repeat"));
        badges.add(new BadgeDto("fifty_bets", "Vétéran",
                "Place 50 paris au total.", totalPlaced >= 50, "medal"));
        badges.add(new BadgeDto("hot_streak_3", "Main chaude",
                "Gagne 3 paris d'affilée.", winStreak >= 3, "flame"));
        badges.add(new BadgeDto("hot_streak_5", "Sur un nuage",
                "Gagne 5 paris d'affilée.", winStreak >= 5, "cloud"));
        badges.add(new BadgeDto("sharpshooter", "Sniper",
                "Termine au moins 10 paris avec 60% de réussite.",
                totalResolved >= SHARPSHOOTER_MIN_BETS && winRate >= SHARPSHOOTER_MIN_WIN_RATE, "target"));
        badges.add(new BadgeDto("parlay_master", "Roi du multiple",
                "Combine au moins 3 sélections dans un seul ticket.", hasParlay, "crown"));
        badges.add(new BadgeDto("big_win", "Gros coup",
                "Remporte un ticket rapportant au moins 500 pts.", hasBigWin, "zap"));
        badges.add(new BadgeDto("grinder", "Gros joueur",
                "Mise un total de 2000 pts sur l'ensemble de tes tickets.",
                totalStaked >= GRINDER_STAKE_THRESHOLD, "coins"));

        return badges;
    }

    /**
     * Série de victoires en cours (0 si le dernier pari résolu est perdu, ou
     * si aucun pari n'est encore résolu). Exposée à part de computeBadges
     * pour l'afficher en direct sur le dashboard (cf. AuthController) : un
     * badge est un simple booléen "débloqué", pas le compteur vivant.
     */
    public int computeWinStreak(List<Bet> betsOrderedMostRecentFirst) {
        int streak = 0;
        for (Bet bet : betsOrderedMostRecentFirst) {
            if (bet.getStatus() == BetStatus.PENDING || bet.getStatus() == BetStatus.VOID) {
                continue;
            }
            if (bet.getStatus() == BetStatus.WON) {
                streak++;
                continue;
            }
            break; // LOST : la série s'arrête ici
        }
        return streak;
    }
}