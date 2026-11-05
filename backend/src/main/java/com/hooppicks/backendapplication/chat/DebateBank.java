package com.hooppicks.backendapplication.chat;

import java.time.LocalDate;
import java.util.List;

/**
 * Les questions du débat du jour quand aucun admin n'en a prévu : elles tournent
 * dans l'ordre, sans en répéter une avant d'avoir fait le tour. Des goûts plutôt
 * que des faits : une question qui se vérifie ne fait pas débattre.
 */
final class DebateBank {

    record Question(String text, String a, String b) {}

    static final List<Question> QUESTIONS = List.of(
            new Question("Le plus grand de tous les temps ?", "Jordan", "LeBron"),
            new Question("Meilleur joueur du monde aujourd'hui ?", "Jokic", "Wembanyama"),
            new Question("Les matchs de la nuit : en direct ou le résumé au réveil ?", "En direct", "Le résumé"),
            new Question("Le 3 points a-t-il rendu la NBA meilleure ?", "Oui", "Non"),
            new Question("Dernier tir, match 7 : tu donnes le ballon à qui ?", "Jordan", "Kobe"),
            new Question("Meilleur joueur né en Afrique de l'histoire ?", "Olajuwon", "Embiid"),
            new Question("Ton style de ticket ?", "Le sûr", "Le gros coup"),
            new Question("Les Bulls de 1996 contre les Warriors de 2017 : qui gagne ?", "Bulls", "Warriors"),
            new Question("Tu construis ton équipe autour de qui ?", "Wembanyama", "Doncic"),
            new Question("82 matchs de saison régulière, c'est trop ?", "Trop", "Parfait"),
            new Question("Meilleur shooteur de l'histoire ?", "Curry", "Ray Allen"),
            new Question("Durant a-t-il eu raison de rejoindre Golden State ?", "Oui", "Non"),
            new Question("Pour gagner un titre, il faut d'abord ?", "L'attaque", "La défense"),
            new Question("Le patron des Lakers des années 2000 ?", "Kobe", "Shaq"),
            new Question("Le play-in : à garder ?", "À garder", "À supprimer"),
            new Question("Un combiné à 8 sélections ?", "Du génie", "De la folie"),
            new Question("Le meilleur en playoffs aujourd'hui ?", "Giannis", "Jokic"),
            new Question("Le plus fort des deux ?", "Magic", "Bird"),
            new Question("Une équipe de stars ou une équipe construite par la draft ?", "Les stars", "La draft"),
            new Question("L'All-Star Game : à sauver ?", "Oui", "Plus la peine"),
            new Question("Le visage de la NBA dans 5 ans ?", "Wembanyama", "Edwards"),
            new Question("LeBron a-t-il bien fait de quitter Cleveland en 2010 ?", "Oui", "Non"),
            new Question("Meilleur meneur de l'histoire ?", "Magic", "Curry"),
            new Question("Le chambrage sur le terrain ?", "Ça fait le jeu", "Trop, c'est trop"),
            new Question("Faut-il reculer la ligne à 3 points ?", "Oui", "Non"),
            new Question("Le meilleur dunkeur de l'histoire ?", "Vince Carter", "Jordan"),
            new Question("Un contre un au sommet de leur forme ?", "Olajuwon", "Shaq"),
            new Question("La Coupe NBA, ça te fait vibrer ?", "Oui", "Pas vraiment"),
            new Question("Pour juger un joueur : les stats ou les yeux ?", "Les stats", "Les yeux"),
            new Question("Meilleur arrière actuel ?", "Gilgeous-Alexander", "Edwards"),
            new Question("Le meilleur ailier fort de l'histoire ?", "Duncan", "Garnett"),
            new Question("Le MVP doit-il venir d'une des meilleures équipes ?", "Oui", "Non"),
            new Question("Le meilleur défenseur de l'histoire ?", "Olajuwon", "Mutombo"),
            new Question("Tu paries sur ton équipe même quand elle est mal partie ?", "Toujours", "Jamais"),
            new Question("Finale NBA : c'est mieux quand ça va ?", "Jusqu'au match 7", "Un balayage historique"),
            new Question("L'arrière le plus spectaculaire ?", "Iverson", "Wade"),
            new Question("Les récompenses interdites sous 65 matchs joués ?", "Bonne règle", "Mauvaise règle"),
            new Question("La conférence la plus relevée cette saison ?", "L'Est", "L'Ouest"),
            new Question("En playoffs, tu préfères ?", "L'expérience", "La jeunesse"),
            new Question("Un joueur de devoir ou une star qui prend tous les tirs ?", "Le joueur de devoir", "La star"),
            new Question("Le meilleur duo de l'histoire ?", "Jordan et Pippen", "Kobe et Shaq"),
            new Question("L'avantage du terrain compte-t-il encore vraiment ?", "Oui", "Plus tellement"),
            new Question("Le joueur que tu paierais pour voir ?", "Curry", "Wembanyama"),
            new Question("Le meilleur pivot de l'histoire ?", "Abdul-Jabbar", "Shaq"),
            new Question("Le meilleur scoreur pur de l'histoire ?", "Jordan", "Durant"),
            new Question("La vidéo pour les arbitres ?", "Il en faut plus", "Il y en a trop"),
            new Question("Ton équipe favorite : tu la suis même en reconstruction ?", "Jusqu'au bout", "Je zappe"),
            new Question("Un jeune talent ou un vétéran pour finir un match serré ?", "Le jeune", "Le vétéran"),
            new Question("Le plus beau jeu de l'histoire ?", "Warriors 2015-2017", "Spurs 2014"),
            new Question("Un cinq sans pivot peut-il gagner le titre ?", "Oui", "Non"),
            new Question("La plus grande rivalité ?", "Lakers-Celtics", "Bulls-Pistons"),
            new Question("Un rookie peut-il être le meilleur joueur de son équipe en playoffs ?", "Oui", "Non"),
            new Question("Le joueur le plus sous-coté de la ligue ?", "Un meneur", "Un intérieur"),
            new Question("Tu préfères un match à ?", "130-128", "92-88"),
            new Question("Le meilleur passeur de l'histoire ?", "Magic", "Stockton"),
            new Question("Quitter son équipe pour gagner un titre ?", "Normal", "Lâche"));

    private DebateBank() {}

    static Question of(LocalDate day) {
        return QUESTIONS.get((int) Math.floorMod(day.toEpochDay(), (long) QUESTIONS.size()));
    }
}
