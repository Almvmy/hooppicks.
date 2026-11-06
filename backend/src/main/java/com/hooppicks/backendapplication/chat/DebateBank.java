package com.hooppicks.backendapplication.chat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Les questions du débat du jour quand aucun admin n'en a prévu et que l'actualité
 * n'en donne pas (DebateNews) : elles tournent dans l'ordre, sans en répéter une de
 * la saison. Des goûts plutôt que des faits : une question qui se vérifie ne fait
 * pas débattre.
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
            new Question("Quitter son équipe pour gagner un titre ?", "Normal", "Lâche"),
            new Question("Meilleur joueur de l'histoire à ne jamais avoir gagné de titre ?", "Barkley", "Malone"),
            new Question("Le plus grand gâchis de l'histoire à cause des blessures ?", "Derrick Rose", "Grant Hill"),
            new Question("Le meilleur joueur actuel à l'Est ?", "Giannis", "Tatum"),
            new Question("Le meilleur joueur actuel à l'Ouest ?", "Jokic", "Gilgeous-Alexander"),
            new Question("Qui marquera le plus cette saison ?", "Doncic", "Gilgeous-Alexander"),
            new Question("Le meilleur tireur de la ligue aujourd'hui ?", "Curry", "Lillard"),
            new Question("Le meilleur défenseur de la ligue aujourd'hui ?", "Wembanyama", "Gobert"),
            new Question("Le joueur le plus excitant à regarder ?", "Edwards", "Morant"),
            new Question("Le meilleur jeune de moins de 23 ans ?", "Wembanyama", "Holmgren"),
            new Question("La plus belle salle de la ligue ?", "Madison Square Garden", "TD Garden"),
            new Question("Le plus beau maillot de la ligue ?", "Lakers", "Bulls"),
            new Question("Le plus beau logo de la ligue ?", "Bulls", "Raptors"),
            new Question("La meilleure ambiance en playoffs ?", "Knicks", "Warriors"),
            new Question("La franchise la plus mythique ?", "Lakers", "Celtics"),
            new Question("Jordan dans la NBA d'aujourd'hui ?", "Encore meilleur", "Moins dominant"),
            new Question("Curry dans la NBA des années 90 ?", "Il domine pareil", "Il souffre"),
            new Question("Shaq dans la NBA d'aujourd'hui ?", "Injouable", "Sorti du terrain"),
            new Question("La NBA était meilleure ?", "Avant", "Maintenant"),
            new Question("Les années les plus belles de la NBA ?", "Les années 90", "Aujourd'hui"),
            new Question("Le meilleur entraîneur de l'histoire ?", "Phil Jackson", "Popovich"),
            new Question("Le plus gros coup de la dernière décennie ?", "Durant aux Warriors", "LeBron à Cleveland"),
            new Question("Le meilleur tir de l'histoire ?", "Jordan en 1998", "Ray Allen en 2013"),
            new Question("La plus grande finale de l'histoire ?", "Cavaliers-Warriors 2016", "Heat-Spurs 2013"),
            new Question("Le plus beau dunk de l'histoire ?", "Vince Carter sur Weis", "Jordan depuis la ligne"),
            new Question("Le plus grand clutch de l'histoire ?", "Jordan", "Kobe"),
            new Question("Pour une saison entière : LeBron ou Kobe ?", "LeBron", "Kobe"),
            new Question("Pour un match 7 : LeBron ou Kobe ?", "LeBron", "Kobe"),
            new Question("Le meilleur Français de l'histoire de la NBA ?", "Tony Parker", "Wembanyama"),
            new Question("Le meilleur Européen de l'histoire ?", "Jokic", "Nowitzki"),
            new Question("Le meilleur étranger de l'histoire ?", "Olajuwon", "Jokic"),
            new Question("Le meilleur joueur d'origine africaine aujourd'hui ?", "Giannis", "Embiid"),
            new Question("Les triple-doubles, ça veut dire quelque chose ?", "Oui", "Stat gonflée"),
            new Question("Tu préfères gagner 1 titre ou 5 fois MVP ?", "1 titre", "5 fois MVP"),
            new Question("Un joueur doit-il rester toute sa carrière au même club ?", "Oui, c'est ça la légende", "Non, il va où il gagne"),
            new Question("Une star peut-elle réclamer son transfert ?", "C'est son droit", "Il respecte son contrat"),
            new Question("Se reposer un soir de match quand on n'est pas blessé ?", "Normal", "Inacceptable"),
            new Question("Le tanking, perdre exprès pour la draft ?", "Malin", "Honteux"),
            new Question("Les arbitres avantagent-ils les stars ?", "Évidemment", "Pas tant que ça"),
            new Question("Le meilleur cinq de départ de l'histoire ?", "Warriors 2017", "Bulls 1996"),
            new Question("Tu commences une équipe avec un meneur ou un pivot ?", "Un meneur", "Un pivot"),
            new Question("Plus important pour gagner ?", "Le talent", "Le collectif"),
            new Question("Un gros contrat, ça change un joueur ?", "Oui", "Non"),
            new Question("Le plus dur à défendre, en un contre un ?", "Durant", "Irving"),
            new Question("Le meilleur dribbleur de l'histoire ?", "Kyrie Irving", "Allen Iverson"),
            new Question("Le meilleur passeur actuel ?", "Jokic", "Haliburton"),
            new Question("Le meilleur rebondeur de l'histoire ?", "Rodman", "Chamberlain"),
            new Question("Le meilleur contreur de l'histoire ?", "Mutombo", "Olajuwon"),
            new Question("Le joueur le plus sous-estimé de l'histoire ?", "Pippen", "Hakeem"),
            new Question("Le joueur le plus surcoté de la ligue ?", "Un arrière star", "Un pivot star"),
            new Question("Wilt Chamberlain aujourd'hui, il marque 100 points ?", "Oui", "Jamais"),
            new Question("Une équipe d'Abidjan en NBA un jour ?", "Oui, on y croit", "Pas de sitôt"),
            new Question("Un match NBA à Abidjan, tu y vas à quel prix ?", "À n'importe quel prix", "Ça dépend"),
            new Question("Tu veilles pour un match à 2h du matin un soir de semaine ?", "Toujours", "Seulement en playoffs"),
            new Question("Regarder un match sans savoir le score, au réveil ?", "Sacré", "Je regarde le score d'abord"),
            new Question("Tu regardes plutôt ?", "Les matchs entiers", "Les résumés"),
            new Question("La saison régulière a-t-elle encore de l'intérêt ?", "Oui", "Seuls les playoffs comptent"),
            new Question("Tes paris : avec le cœur ou avec la tête ?", "Le cœur", "La tête"),
            new Question("Parier contre ton équipe favorite ?", "Jamais", "Si la cote est belle"),
            new Question("Le plus dur à pronostiquer ?", "Les favoris à domicile", "Les paris joueurs"),
            new Question("Tu préfères ?", "Une petite cote sûre", "Une grosse cote folle"),
            new Question("Tu changes d'équipe favorite si elle déçoit ?", "Jamais", "Ça m'est arrivé"),
            new Question("Le meilleur coéquipier de l'histoire de LeBron ?", "Wade", "Irving"),
            new Question("Le meilleur partenaire de Jordan après Pippen ?", "Rodman", "Kukoc"),
            new Question("Le meilleur joueur de l'histoire des Warriors ?", "Curry", "Chamberlain"),
            new Question("Le meilleur joueur de l'histoire des Lakers ?", "Kobe", "Magic"),
            new Question("Le meilleur joueur de l'histoire des Celtics ?", "Bird", "Russell"),
            new Question("Le meilleur joueur de l'histoire des Spurs ?", "Duncan", "Robinson"),
            new Question("Le meilleur joueur de l'histoire des Knicks ?", "Ewing", "Frazier"),
            new Question("Le meilleur joueur de l'histoire du Heat ?", "Wade", "LeBron"),
            new Question("Le meilleur joueur de l'histoire des 76ers ?", "Iverson", "Erving"),
            new Question("Le meilleur joueur de l'histoire des Rockets ?", "Olajuwon", "Harden"),
            new Question("Le meilleur joueur de l'histoire des Bucks ?", "Giannis", "Abdul-Jabbar"),
            new Question("Les playoffs à 7 matchs, le premier tour aussi ?", "Oui", "5 suffiraient"),
            new Question("Une finale de Coupe NBA vaut-elle un titre ?", "Presque", "Pas du tout"),
            new Question("Un joueur peut-il être MVP dans une équipe hors playoffs ?", "Oui", "Non"),
            new Question("Faut-il supprimer le tir à 3 points dans les coins ?", "Oui", "Non"),
            new Question("Faut-il passer à 4 points pour les tirs très lointains ?", "Oui", "Non, jamais"),
            new Question("Une ligue européenne de la NBA, bonne idée ?", "Oui", "Non"),
            new Question("Une équipe NBA en Afrique, bonne idée ?", "Oui", "Trop tôt"),
            new Question("Les joueurs d'aujourd'hui sont-ils trop protégés ?", "Oui", "Non"),
            new Question("Le plus beau geste du basket ?", "Le dunk", "Le crossover"),
            new Question("Ce qui fait le plus lever la salle ?", "Le tir au buzzer", "Le contre sur la planche"),
            new Question("Le meilleur moment d'un match ?", "Le dernier quart", "Le coup d'envoi"),
            new Question("Tu préfères être ?", "Le joueur du dernier tir", "Le meilleur défenseur"),
            new Question("Tu choisis ton cinq : expérience ou athlétisme ?", "Expérience", "Athlétisme"),
            new Question("Le meilleur joueur de l'histoire des Bulls après Jordan ?", "Pippen", "Rose"),
            new Question("Le meilleur joueur de l'histoire des Mavericks ?", "Nowitzki", "Doncic"),
            new Question("Le meilleur joueur de l'histoire des Nuggets ?", "Jokic", "Anthony"),
            new Question("Le meilleur joueur de l'histoire des Suns ?", "Nash", "Barkley"),
            new Question("Le meilleur joueur de l'histoire du Thunder ?", "Durant", "Gilgeous-Alexander"),
            new Question("Une dynastie, c'est mieux ou c'est lassant ?", "C'est beau", "C'est lassant"),
            new Question("Mieux vaut un titre surprise ou une dynastie de ton équipe ?", "Un titre surprise", "Une dynastie"),
            new Question("Le pire : perdre en finale ou rater les playoffs ?", "Perdre en finale", "Rater les playoffs"),
            new Question("Tu quittes le match à 20 points d'écart à la mi-temps ?", "Je reste", "Je coupe"),
            new Question("Une prolongation à 3h du matin ?", "Le bonheur", "La souffrance"));

    private DebateBank() {}

    static Question of(LocalDate day) {
        return QUESTIONS.get((int) Math.floorMod(day.toEpochDay(), (long) QUESTIONS.size()));
    }

    /** La première question pas encore sortie, à partir de celle du jour. Si tout est sorti, celle du jour. */
    static Question next(LocalDate day, Set<String> used) {
        int start = (int) Math.floorMod(day.toEpochDay(), (long) QUESTIONS.size());
        for (int i = 0; i < QUESTIONS.size(); i++) {
            Question q = QUESTIONS.get((start + i) % QUESTIONS.size());
            if (!used.contains(q.text() + "|" + q.a())) return q;
        }
        return of(day);
    }

    /** Clé d'une question déjà sortie : le texte seul ne suffit pas, deux questions peuvent le partager. */
    static String key(String text, String a) {
        return text + "|" + a;
    }
}
