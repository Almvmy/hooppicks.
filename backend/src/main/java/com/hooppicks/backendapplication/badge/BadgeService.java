package com.hooppicks.backendapplication.badge;

import com.hooppicks.backendapplication.dto.BadgeDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.favorite.FavoriteTeamService;
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
    private static final int LOYAL_FAN_SELECTIONS = 10;

    /**
     * Calcule le catalogue de badges pour un utilisateur à partir de ses paris.
     * bets doit être trié du plus récent au plus ancien (comme le renvoie
     * BetRepository#findByUserIdOrderByPlacedAtDesc), car la série de victoires
     * dépend de cet ordre.
     */
    public List<BadgeDto> computeBadges(List<Bet> bets) {
        return computeBadges(bets, FavoriteTeamService.BadgeFacts.NONE);
    }

    /**
     * Variante avec les faits liés à l'équipe favorite (cf.
     * FavoriteTeamService#badgeFacts), calculés sur l'équipe favorite
     * actuelle : en changer peut donc reverrouiller ces deux badges, ce qui
     * est cohérent avec leur intitulé (« ton équipe favorite »).
     */
    public List<BadgeDto> computeBadges(List<Bet> bets, FavoriteTeamService.BadgeFacts favorite) {
        return computeBadges(bets, favorite, false);
    }

    /** wonAgainstCrowd : cf. CrowdService (calcul à part, il lit la répartition des pronostics). */
    public List<BadgeDto> computeBadges(List<Bet> bets, FavoriteTeamService.BadgeFacts favorite, boolean wonAgainstCrowd) {
        long totalPlaced = bets.size();
        long totalResolved = bets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .count();
        long wonCount = bets.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
        int winRate = totalResolved == 0 ? 0 : (int) Math.round((wonCount * 100.0) / totalResolved);

        // Meilleure série atteinte, pas la série en cours : un badge débloqué
        // ne doit pas se reverrouiller à la première défaite.
        int bestStreak = computeBestWinStreak(bets);
        boolean hasParlay = bets.stream().anyMatch(b -> b.getSelections().size() >= PARLAY_MIN_SELECTIONS);
        boolean hasBigWin = bets.stream()
                // Bénéfice, pas gain brut : miser 500 sur une cote à 1,03 suffisait.
                .anyMatch(b -> b.getStatus() == BetStatus.WON && b.getPotentialPayout() - b.getStake() >= BIG_WIN_THRESHOLD);
        int totalStaked = bets.stream().mapToInt(Bet::getStake).sum();

        List<BadgeDto> badges = new ArrayList<>();
        badges.add(new BadgeDto("first_bet", "Premier ticket",
                "Place ton tout premier pari.", totalPlaced >= 1, "ticket"));
        badges.add(new BadgeDto("ten_bets", "Habitué",
                "Place 10 paris au total.", totalPlaced >= 10, "repeat"));
        badges.add(new BadgeDto("fifty_bets", "Vétéran",
                "Place 50 paris au total.", totalPlaced >= 50, "medal"));
        badges.add(new BadgeDto("hot_streak_3", "Main chaude",
                "Gagne 3 paris d'affilée.", bestStreak >= 3, "flame"));
        badges.add(new BadgeDto("hot_streak_5", "Sur un nuage",
                "Gagne 5 paris d'affilée.", bestStreak >= 5, "cloud"));
        badges.add(new BadgeDto("sharpshooter", "Sniper",
                "Termine au moins 10 paris avec 60% de réussite.",
                totalResolved >= SHARPSHOOTER_MIN_BETS && winRate >= SHARPSHOOTER_MIN_WIN_RATE, "target"));
        badges.add(new BadgeDto("parlay_master", "Roi du multiple",
                "Combine au moins 3 sélections dans un seul ticket.", hasParlay, "crown"));
        badges.add(new BadgeDto("big_win", "Gros coup",
                "Remporte un ticket avec au moins 500 pts de bénéfice.", hasBigWin, "zap"));
        badges.add(new BadgeDto("grinder", "Gros joueur",
                "Mise un total de 2000 pts sur l'ensemble de tes tickets.",
                totalStaked >= GRINDER_STAKE_THRESHOLD, "coins"));
        badges.add(new BadgeDto("loyal_fan", "Fidèle",
                "Place 10 sélections en faveur de ton équipe favorite.",
                favorite.forSelections() >= LOYAL_FAN_SELECTIONS, "heart"));
        badges.add(new BadgeDto("clear_eyed", "Lucide",
                "Gagne un ticket en pariant contre ton équipe favorite.",
                favorite.wonTicketAgainst(), "eye"));
        badges.add(new BadgeDto("contrarian", "Contre la foule",
                "Gagne une sélection que moins de 20 % des joueurs avaient choisie.", wonAgainstCrowd, "users"));

        return badges;
    }

    /**
     * Série de victoires en cours (0 si le dernier pari résolu est perdu, ou
     * si aucun pari n'est encore résolu). Exposée à part de computeBadges
     * pour l'afficher en direct sur le dashboard (cf. AuthController) : un
     * badge est un simple booléen "débloqué", pas le compteur vivant.
     */
    /** Plus longue série de tickets gagnés d'affilée (remboursés et en attente ignorés). */
    public int computeBestWinStreak(List<Bet> betsOrderedMostRecentFirst) {
        int best = 0, current = 0;
        for (Bet bet : betsOrderedMostRecentFirst) {
            if (bet.getStatus() == BetStatus.WON) best = Math.max(best, ++current);
            else if (bet.getStatus() == BetStatus.LOST) current = 0;
        }
        return best;
    }

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