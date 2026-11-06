package com.hooppicks.backendapplication.chat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Un débat tiré de l'actualité, les jours où il s'est passé quelque chose : un soir
 * où quelqu'un marque 60 points, on ne débat pas de Jordan contre LeBron. Rien en
 * base ici, tout arrive déjà lu.
 *
 * Pas un débat d'actualité tous les jours (DebateService en laisse un sur deux) : en
 * saison, il y a presque toujours un joueur à 40 points, et « X à 42 points » chaque
 * matin se répéterait autant que la liste.
 */
final class DebateNews {

    record Performance(String player, int points, int rebounds, int assists) {}

    record TeamForm(String team, String streak) {}

    record News(String kind, String subject, String question, String optionA, String optionB) {}

    private static final int BIG_NIGHT = 40;
    private static final int HUGE_NIGHT = 50;
    private static final int TRIPLE_DOUBLE_POINTS = 25;
    private static final int STREAK = 6;

    private DebateNews() {}

    /** `blockedKinds` : types déjà sortis ces derniers jours. `recentQuestions` : pour ne pas reparler du même joueur ou de la même équipe. */
    static Optional<News> pick(LocalDate day, List<Performance> night, List<TeamForm> teams, Set<String> blockedKinds, List<String> recentQuestions) {
        List<Performance> byPoints = night.stream().sorted(Comparator.comparingInt(Performance::points).reversed()).toList();
        List<News> candidates = new ArrayList<>();
        for (Performance p : byPoints) {
            if (p.points() >= HUGE_NIGHT) {
                candidates.add(new News("exploit", p.player(), p.player() + " à " + p.points() + " points la nuit dernière : la performance de la saison ?", "Oui", "On verra mieux"));
            }
        }
        for (Performance p : byPoints) {
            if (p.points() >= TRIPLE_DOUBLE_POINTS && p.rebounds() >= 10 && p.assists() >= 10) {
                candidates.add(new News("triple", p.player(), "Triple-double de " + p.player() + " (" + p.points() + "-" + p.rebounds() + "-" + p.assists() + ") : candidat au titre de MVP ?", "Oui", "Pas encore"));
            }
        }
        for (Performance p : byPoints) {
            if (p.points() >= BIG_NIGHT && p.points() < HUGE_NIGHT) {
                candidates.add(bigNight(day, p));
            }
        }
        for (TeamForm t : teams) {
            int wins = streak(t, 'W');
            if (wins >= STREAK) candidates.add(new News("serie", t.team(), "Les " + t.team() + " enchaînent " + wins + " victoires : candidats au titre ?", "Oui", "Feu de paille"));
            int losses = streak(t, 'L');
            if (losses >= STREAK) candidates.add(new News("crise", t.team(), "Les " + t.team() + " ont perdu " + losses + " matchs de suite : il faut tout changer ?", "Tout changer", "Patience"));
        }
        return candidates.stream()
                .filter(n -> !blockedKinds.contains(n.kind()))
                .filter(n -> recentQuestions.stream().noneMatch(q -> q.contains(n.subject())))
                .findFirst();
    }

    // Trois tournures, pour qu'un soir à 40 points ne ressemble pas au précédent.
    private static News bigNight(LocalDate day, Performance p) {
        String intro = p.player() + " à " + p.points() + " points";
        return switch ((int) Math.floorMod(day.toEpochDay(), 3L)) {
            case 0 -> new News("gros-match", p.player(), intro + " : le meilleur à son poste en ce moment ?", "Oui", "Non");
            case 1 -> new News("gros-match", p.player(), intro + " : tu le prends dans ton équipe de rêve ?", "Direct", "Pas sûr");
            default -> new News("gros-match", p.player(), intro + " : vraie star ou gros soir ?", "Vraie star", "Gros soir");
        };
    }

    private static int streak(TeamForm t, char side) {
        String s = t.streak();
        return s != null && s.length() > 1 && s.charAt(0) == side && s.substring(1).matches("\\d+") ? Integer.parseInt(s.substring(1)) : 0;
    }
}
