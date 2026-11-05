package com.hooppicks.backendapplication.quiz;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Questions d'histoire et de règles : elles ne dépendent d'aucune donnée, donc le
 * quiz reste jouable sans match la veille. Bonne réponse en premier, l'ordre est
 * mélangé à la génération. Seulement des faits arrêtés (pas de « le plus titré »,
 * qui peut changer). Deux par jour, sans répétition avant d'avoir fait le tour.
 */
final class QuizBank {

    record Item(String text, List<String> choices) {}

    private static Item q(String text, String... choices) {
        return new Item(text, List.of(choices));
    }

    static final List<Item> ITEMS = List.of(
            q("Combien de titres NBA Michael Jordan a-t-il gagnés ?", "6", "5", "4", "7"),
            q("Contre quelle équipe Kobe Bryant a-t-il marqué 81 points ?", "Raptors", "Suns", "Knicks", "Mavericks"),
            q("Combien de points Wilt Chamberlain a-t-il marqués dans un seul match en 1962 ?", "100", "81", "92", "73"),
            q("Premier choix de la draft 2003 ?", "LeBron James", "Carmelo Anthony", "Dwyane Wade", "Chris Bosh"),
            q("Premier choix de la draft 2023 ?", "Victor Wembanyama", "Brandon Miller", "Scoot Henderson", "Amen Thompson"),
            q("À quel rang Michael Jordan a-t-il été drafté en 1984 ?", "3e", "1er", "2e", "5e"),
            q("Dans quel pays est né Hakeem Olajuwon ?", "Nigeria", "Cameroun", "Sénégal", "Ghana"),
            q("Dans quel pays est né Joel Embiid ?", "Cameroun", "Nigeria", "Gabon", "Sénégal"),
            q("Dans quel pays est né Dikembe Mutombo ?", "RD Congo", "Sénégal", "Nigeria", "Cameroun"),
            q("Pour quel pays joue Giannis Antetokounmpo ?", "Grèce", "Nigeria", "Turquie", "Italie"),
            q("De quel pays vient Nikola Jokic ?", "Serbie", "Croatie", "Slovénie", "Monténégro"),
            q("De quel pays vient Luka Doncic ?", "Slovénie", "Serbie", "Croatie", "Lettonie"),
            q("De quel pays vient Dirk Nowitzki ?", "Allemagne", "Autriche", "Lituanie", "Suisse"),
            q("De quel pays vient Manu Ginobili ?", "Argentine", "Espagne", "Italie", "Brésil"),
            q("De quel pays vient Yao Ming ?", "Chine", "Japon", "Corée du Sud", "Philippines"),
            q("De quel pays vient Pau Gasol ?", "Espagne", "Argentine", "France", "Italie"),
            q("De quel pays vient Victor Wembanyama ?", "France", "Belgique", "Canada", "Suisse"),
            q("Avec quelle équipe Larry Bird a-t-il fait toute sa carrière ?", "Celtics", "Lakers", "Pacers", "Knicks"),
            q("Avec quelle équipe John Stockton a-t-il fait toute sa carrière ?", "Jazz", "Blazers", "Suns", "Kings"),
            q("Avec quelle équipe Tim Duncan a-t-il fait toute sa carrière ?", "Spurs", "Rockets", "Mavericks", "Heat"),
            q("Avec quelle équipe Reggie Miller a-t-il fait toute sa carrière ?", "Pacers", "Knicks", "Bulls", "Pistons"),
            q("Avec quelle équipe Dirk Nowitzki a-t-il fait toute sa carrière ?", "Mavericks", "Spurs", "Rockets", "Nuggets"),
            q("Avec quelle équipe Kobe Bryant a-t-il fait toute sa carrière ?", "Lakers", "Hornets", "Clippers", "76ers"),
            q("Quelle équipe a drafté Shaquille O'Neal en 1992 ?", "Magic", "Lakers", "Heat", "Suns"),
            q("Quelle équipe a drafté Kevin Durant en 2007 ?", "SuperSonics", "Thunder", "Warriors", "Trail Blazers"),
            q("Champion NBA en 2008 ?", "Celtics", "Lakers", "Spurs", "Pistons"),
            q("Champion NBA en 2011 ?", "Mavericks", "Heat", "Lakers", "Thunder"),
            q("Champion NBA en 2014 ?", "Spurs", "Heat", "Thunder", "Pacers"),
            q("Champion NBA en 2016 ?", "Cavaliers", "Warriors", "Thunder", "Raptors"),
            q("Champion NBA en 2019 ?", "Raptors", "Warriors", "Bucks", "Rockets"),
            q("Champion NBA en 2020 ?", "Lakers", "Heat", "Nuggets", "Celtics"),
            q("Champion NBA en 2021 ?", "Bucks", "Suns", "Hawks", "Clippers"),
            q("Champion NBA en 2023 ?", "Nuggets", "Heat", "Lakers", "Celtics"),
            q("Champion NBA en 2024 ?", "Celtics", "Mavericks", "Timberwolves", "Pacers"),
            q("Champion NBA en 2025 ?", "Thunder", "Pacers", "Knicks", "Timberwolves"),
            q("Combien de minutes dure un quart-temps en NBA ?", "12", "10", "15", "20"),
            q("Combien de secondes pour tirer sur une attaque en NBA ?", "24", "30", "20", "14"),
            q("Combien de fautes personnelles avant l'exclusion en NBA ?", "6", "5", "4", "7"),
            q("À quelle hauteur se trouve le panier ?", "3,05 m", "2,90 m", "3,15 m", "3,25 m"),
            q("Combien de matchs joue une équipe en saison régulière ?", "82", "72", "90", "100"),
            q("Combien d'équipes compte la NBA ?", "30", "28", "32", "36"),
            q("Une série de playoffs NBA se joue au meilleur de combien de matchs ?", "7", "5", "3", "9"),
            q("Quel numéro portait Michael Jordan aux Bulls, la plupart du temps ?", "23", "45", "33", "32"),
            q("Quels numéros Kobe Bryant a-t-il portés aux Lakers ?", "8 et 24", "8 et 23", "24 et 33", "10 et 24"),
            q("Quel joueur a inspiré le logo de la NBA ?", "Jerry West", "Michael Jordan", "Bill Russell", "Bob Cousy"),
            q("Qui surnomme-t-on « The Greek Freak » ?", "Giannis Antetokounmpo", "Nikola Vucevic", "Kristaps Porzingis", "Rudy Gobert"),
            q("Qui surnommait-on « The Answer » ?", "Allen Iverson", "Tracy McGrady", "Vince Carter", "Steve Francis"),
            q("Qui surnommait-on « Black Mamba » ?", "Kobe Bryant", "Michael Jordan", "Paul Pierce", "Dwyane Wade"),
            q("Qui surnomme-t-on « The Beard » ?", "James Harden", "Stephen Curry", "Russell Westbrook", "Damian Lillard"),
            q("Qui surnomme-t-on « The Joker » ?", "Nikola Jokic", "Luka Doncic", "Joel Embiid", "Kyrie Irving"),
            q("Qui surnommait-on « The Dream » ?", "Hakeem Olajuwon", "David Robinson", "Patrick Ewing", "Clyde Drexler"),
            q("Qui surnommait-on « The Mailman » ?", "Karl Malone", "John Stockton", "Charles Barkley", "Dennis Rodman"),
            q("Qui surnommait-on « The Admiral » ?", "David Robinson", "Shaquille O'Neal", "Tim Duncan", "Alonzo Mourning"),
            q("Quel est le vrai prénom de Magic Johnson ?", "Earvin", "Michael", "Marcus", "Ervin"),
            q("Avec quelle équipe Stephen Curry a-t-il débuté en NBA ?", "Warriors", "Hornets", "Knicks", "Suns"),
            q("Avec quelle équipe LeBron James a-t-il débuté en NBA ?", "Cavaliers", "Heat", "Lakers", "Knicks"),
            q("Avec quelle équipe Tony Parker a-t-il gagné ses 4 titres ?", "Spurs", "Hornets", "Lakers", "Heat"),
            q("Face à qui les Cavaliers ont-ils remonté un 3-1 en finale 2016 ?", "Warriors", "Spurs", "Thunder", "Raptors"),
            q("Combien de titres Tim Duncan a-t-il gagnés ?", "5", "4", "6", "3"),
            q("Combien de joueurs par équipe sur le terrain ?", "5", "6", "4", "7"));

    // Un ordre fixe mais mélangé, pour ne pas enchaîner « Champion en 2008, en 2011… ».
    private static final List<Item> SHUFFLED = shuffled();

    private QuizBank() {}

    private static List<Item> shuffled() {
        List<Item> items = new ArrayList<>(ITEMS);
        Collections.shuffle(items, new Random(2026));
        return List.copyOf(items);
    }

    /** La n-ième question du jour (0 ou 1, on en prend deux par jour, plus si les données manquent). */
    static Item of(LocalDate day, int n) {
        return SHUFFLED.get((int) Math.floorMod(day.toEpochDay() * 2 + n, (long) SHUFFLED.size()));
    }
}
