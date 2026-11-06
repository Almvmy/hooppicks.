package com.hooppicks.backendapplication.quiz;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Questions d'histoire et de règles : elles ne dépendent d'aucune donnée, donc le
 * quiz reste jouable sans match la veille. Bonne réponse en premier, l'ordre est
 * mélangé à la génération. Seulement des faits arrêtés (pas de « le plus titré »,
 * qui peut changer). Une question ne revient pas de la saison tant qu'il en reste,
 * et jamais deux du même thème le même jour : la liste des débuts, 60 questions dont
 * 10 « Champion en… », se retenait en un mois.
 */
final class QuizBank {

    record Item(String theme, String text, List<String> choices) {}

    private static Item q(String theme, String text, String... choices) {
        return new Item(theme, text, List.of(choices));
    }

    static final List<Item> ITEMS = List.of(
            // Records et grands matchs
            q("records", "Combien de points Wilt Chamberlain a-t-il marqués dans un seul match en 1962 ?", "100", "81", "92", "73"),
            q("records", "Contre quelle équipe Kobe Bryant a-t-il marqué 81 points ?", "Raptors", "Suns", "Knicks", "Mavericks"),
            q("records", "Combien de points Kobe Bryant a-t-il marqués lors de son dernier match ?", "60", "42", "51", "37"),
            q("records", "Combien de victoires ont les Warriors en saison régulière 2015-16 ?", "73", "72", "70", "69"),
            q("records", "Combien de victoires ont les Bulls en saison régulière 1995-96 ?", "72", "73", "69", "67"),
            q("records", "Qui a marqué 37 points en un seul quart-temps en 2015 ?", "Klay Thompson", "Stephen Curry", "James Harden", "Kevin Durant"),
            q("records", "Qui a réussi 14 tirs à 3 points dans un match en 2018 ?", "Klay Thompson", "Stephen Curry", "Damian Lillard", "Ray Allen"),
            q("records", "Qui a marqué 70 points contre les Celtics en 2017 ?", "Devin Booker", "James Harden", "Anthony Davis", "Russell Westbrook"),
            q("records", "Qui a marqué 73 points contre les Hawks en janvier 2024 ?", "Luka Doncic", "Joel Embiid", "Devin Booker", "Jayson Tatum"),
            q("records", "Qui a marqué 70 points contre les Spurs en janvier 2024 ?", "Joel Embiid", "Luka Doncic", "Nikola Jokic", "Giannis Antetokounmpo"),
            q("records", "Quel record LeBron James a-t-il pris à Kareem Abdul-Jabbar en 2023 ?", "Le plus de points en carrière", "Le plus de passes", "Le plus de rebonds", "Le plus de titres"),
            q("records", "Combien de titres Bill Russell a-t-il gagnés ?", "11", "8", "9", "13"),
            q("records", "Qui a tourné en triple-double de moyenne sur la saison 2016-17 ?", "Russell Westbrook", "James Harden", "LeBron James", "Nikola Jokic"),
            q("records", "Face à qui les Cavaliers ont-ils remonté un 3-1 en finale 2016 ?", "Warriors", "Spurs", "Thunder", "Raptors"),
            // Champions
            q("champions", "Champion NBA en 2004 ?", "Pistons", "Lakers", "Spurs", "Pacers"),
            q("champions", "Champion NBA en 2006 ?", "Heat", "Mavericks", "Spurs", "Pistons"),
            q("champions", "Champion NBA en 2007 ?", "Spurs", "Cavaliers", "Suns", "Mavericks"),
            q("champions", "Champion NBA en 2008 ?", "Celtics", "Lakers", "Spurs", "Pistons"),
            q("champions", "Champion NBA en 2009 ?", "Lakers", "Magic", "Celtics", "Nuggets"),
            q("champions", "Champion NBA en 2011 ?", "Mavericks", "Heat", "Lakers", "Thunder"),
            q("champions", "Champion NBA en 2012 ?", "Heat", "Thunder", "Spurs", "Celtics"),
            q("champions", "Champion NBA en 2014 ?", "Spurs", "Heat", "Thunder", "Pacers"),
            q("champions", "Champion NBA en 2015 ?", "Warriors", "Cavaliers", "Rockets", "Hawks"),
            q("champions", "Champion NBA en 2016 ?", "Cavaliers", "Warriors", "Thunder", "Raptors"),
            q("champions", "Champion NBA en 2019 ?", "Raptors", "Warriors", "Bucks", "Rockets"),
            q("champions", "Champion NBA en 2020 ?", "Lakers", "Heat", "Nuggets", "Celtics"),
            q("champions", "Champion NBA en 2021 ?", "Bucks", "Suns", "Hawks", "Clippers"),
            q("champions", "Champion NBA en 2022 ?", "Warriors", "Celtics", "Mavericks", "Heat"),
            q("champions", "Champion NBA en 2023 ?", "Nuggets", "Heat", "Lakers", "Celtics"),
            q("champions", "Champion NBA en 2024 ?", "Celtics", "Mavericks", "Timberwolves", "Pacers"),
            q("champions", "Champion NBA en 2025 ?", "Thunder", "Pacers", "Knicks", "Timberwolves"),
            q("champions", "Champion NBA en 2026 ?", "Knicks", "Spurs", "Thunder", "Celtics"),
            q("champions", "Vainqueur de la première Coupe NBA, en 2023 ?", "Lakers", "Pacers", "Bucks", "Celtics"),
            q("champions", "Vainqueur de la Coupe NBA 2024 ?", "Bucks", "Thunder", "Hawks", "Rockets"),
            q("champions", "Vainqueur de la Coupe NBA 2025 ?", "Knicks", "Spurs", "Thunder", "Magic"),
            // MVP des finales
            q("mvp-finales", "MVP des finales 2004 ?", "Chauncey Billups", "Ben Wallace", "Kobe Bryant", "Richard Hamilton"),
            q("mvp-finales", "MVP des finales 2006 ?", "Dwyane Wade", "Shaquille O'Neal", "Dirk Nowitzki", "Jason Terry"),
            q("mvp-finales", "MVP des finales 2007 ?", "Tony Parker", "Tim Duncan", "Manu Ginobili", "LeBron James"),
            q("mvp-finales", "MVP des finales 2008 ?", "Paul Pierce", "Kevin Garnett", "Ray Allen", "Kobe Bryant"),
            q("mvp-finales", "MVP des finales 2010 ?", "Kobe Bryant", "Pau Gasol", "Paul Pierce", "Lamar Odom"),
            q("mvp-finales", "MVP des finales 2011 ?", "Dirk Nowitzki", "Jason Terry", "LeBron James", "Dwyane Wade"),
            q("mvp-finales", "MVP des finales 2014 ?", "Kawhi Leonard", "Tim Duncan", "Tony Parker", "Manu Ginobili"),
            q("mvp-finales", "MVP des finales 2015 ?", "Andre Iguodala", "Stephen Curry", "Klay Thompson", "LeBron James"),
            q("mvp-finales", "MVP des finales 2016 ?", "LeBron James", "Kyrie Irving", "Stephen Curry", "Draymond Green"),
            q("mvp-finales", "MVP des finales 2017 ?", "Kevin Durant", "Stephen Curry", "LeBron James", "Klay Thompson"),
            q("mvp-finales", "MVP des finales 2019 ?", "Kawhi Leonard", "Kyle Lowry", "Pascal Siakam", "Stephen Curry"),
            q("mvp-finales", "MVP des finales 2021 ?", "Giannis Antetokounmpo", "Khris Middleton", "Jrue Holiday", "Chris Paul"),
            q("mvp-finales", "MVP des finales 2022 ?", "Stephen Curry", "Klay Thompson", "Andrew Wiggins", "Jayson Tatum"),
            q("mvp-finales", "MVP des finales 2023 ?", "Nikola Jokic", "Jamal Murray", "Jimmy Butler", "Aaron Gordon"),
            q("mvp-finales", "MVP des finales 2024 ?", "Jaylen Brown", "Jayson Tatum", "Luka Doncic", "Derrick White"),
            q("mvp-finales", "MVP des finales 2025 ?", "Shai Gilgeous-Alexander", "Tyrese Haliburton", "Jalen Williams", "Chet Holmgren"),
            // MVP de la saison
            q("mvp", "MVP de la saison 2001 ?", "Allen Iverson", "Shaquille O'Neal", "Tim Duncan", "Kobe Bryant"),
            q("mvp", "MVP de la saison 2008 ?", "Kobe Bryant", "LeBron James", "Chris Paul", "Kevin Garnett"),
            q("mvp", "MVP de la saison 2011 ?", "Derrick Rose", "LeBron James", "Dwight Howard", "Kobe Bryant"),
            q("mvp", "MVP de la saison 2014 ?", "Kevin Durant", "LeBron James", "Blake Griffin", "Joakim Noah"),
            q("mvp", "MVP de la saison 2016 ?", "Stephen Curry", "Kawhi Leonard", "LeBron James", "Russell Westbrook"),
            q("mvp", "MVP de la saison 2017 ?", "Russell Westbrook", "James Harden", "Kawhi Leonard", "LeBron James"),
            q("mvp", "MVP de la saison 2018 ?", "James Harden", "LeBron James", "Anthony Davis", "Damian Lillard"),
            q("mvp", "MVP de la saison 2019 ?", "Giannis Antetokounmpo", "James Harden", "Paul George", "Nikola Jokic"),
            q("mvp", "MVP de la saison 2021 ?", "Nikola Jokic", "Joel Embiid", "Stephen Curry", "Giannis Antetokounmpo"),
            q("mvp", "MVP de la saison 2023 ?", "Joel Embiid", "Nikola Jokic", "Giannis Antetokounmpo", "Jayson Tatum"),
            q("mvp", "MVP de la saison 2025 ?", "Shai Gilgeous-Alexander", "Nikola Jokic", "Giannis Antetokounmpo", "Jayson Tatum"),
            q("mvp", "Qui a été élu MVP à l'unanimité en 2016 ?", "Stephen Curry", "LeBron James", "Kevin Durant", "Kawhi Leonard"),
            // Draft
            q("draft", "Premier choix de la draft 1992 ?", "Shaquille O'Neal", "Alonzo Mourning", "Christian Laettner", "Jim Jackson"),
            q("draft", "Premier choix de la draft 1996 ?", "Allen Iverson", "Kobe Bryant", "Ray Allen", "Marcus Camby"),
            q("draft", "Premier choix de la draft 1997 ?", "Tim Duncan", "Tracy McGrady", "Chauncey Billups", "Keith Van Horn"),
            q("draft", "Premier choix de la draft 2002 ?", "Yao Ming", "Amar'e Stoudemire", "Jay Williams", "Caron Butler"),
            q("draft", "Premier choix de la draft 2003 ?", "LeBron James", "Carmelo Anthony", "Dwyane Wade", "Chris Bosh"),
            q("draft", "Premier choix de la draft 2004 ?", "Dwight Howard", "Emeka Okafor", "Luol Deng", "Andre Iguodala"),
            q("draft", "Premier choix de la draft 2007 ?", "Greg Oden", "Kevin Durant", "Al Horford", "Mike Conley"),
            q("draft", "Premier choix de la draft 2008 ?", "Derrick Rose", "Michael Beasley", "Russell Westbrook", "Kevin Love"),
            q("draft", "Premier choix de la draft 2011 ?", "Kyrie Irving", "Kawhi Leonard", "Klay Thompson", "Derrick Williams"),
            q("draft", "Premier choix de la draft 2012 ?", "Anthony Davis", "Damian Lillard", "Bradley Beal", "Andre Drummond"),
            q("draft", "Premier choix de la draft 2014 ?", "Andrew Wiggins", "Joel Embiid", "Jabari Parker", "Nikola Jokic"),
            q("draft", "Premier choix de la draft 2015 ?", "Karl-Anthony Towns", "D'Angelo Russell", "Kristaps Porzingis", "Devin Booker"),
            q("draft", "Premier choix de la draft 2018 ?", "Deandre Ayton", "Luka Doncic", "Trae Young", "Marvin Bagley"),
            q("draft", "Premier choix de la draft 2019 ?", "Zion Williamson", "Ja Morant", "RJ Barrett", "De'Andre Hunter"),
            q("draft", "Premier choix de la draft 2020 ?", "Anthony Edwards", "LaMelo Ball", "James Wiseman", "Tyrese Haliburton"),
            q("draft", "Premier choix de la draft 2021 ?", "Cade Cunningham", "Jalen Green", "Evan Mobley", "Scottie Barnes"),
            q("draft", "Premier choix de la draft 2022 ?", "Paolo Banchero", "Chet Holmgren", "Jabari Smith", "Keegan Murray"),
            q("draft", "Premier choix de la draft 2023 ?", "Victor Wembanyama", "Brandon Miller", "Scoot Henderson", "Amen Thompson"),
            q("draft", "Premier choix de la draft 2024 ?", "Zaccharie Risacher", "Alexandre Sarr", "Reed Sheppard", "Stephon Castle"),
            q("draft", "Premier choix de la draft 2025 ?", "Cooper Flagg", "Dylan Harper", "VJ Edgecombe", "Ace Bailey"),
            q("draft", "À quel rang Michael Jordan a-t-il été drafté en 1984 ?", "3e", "1er", "2e", "5e"),
            q("draft", "À quel rang Kobe Bryant a-t-il été drafté en 1996 ?", "13e", "1er", "5e", "24e"),
            q("draft", "À quel rang Nikola Jokic a-t-il été drafté en 2014 ?", "41e", "15e", "4e", "27e"),
            q("draft", "À quel rang Giannis Antetokounmpo a-t-il été drafté en 2013 ?", "15e", "3e", "41e", "8e"),
            q("draft", "À quel rang Stephen Curry a-t-il été drafté en 2009 ?", "7e", "1er", "3e", "15e"),
            q("draft", "Quelle équipe a drafté Kobe Bryant en 1996 ?", "Hornets", "Lakers", "Nets", "Celtics"),
            q("draft", "Quelle équipe a drafté Shaquille O'Neal en 1992 ?", "Magic", "Lakers", "Heat", "Suns"),
            q("draft", "Quelle équipe a drafté Kevin Durant en 2007 ?", "SuperSonics", "Thunder", "Warriors", "Trail Blazers"),
            // Pays
            q("pays", "Dans quel pays est né Hakeem Olajuwon ?", "Nigeria", "Cameroun", "Sénégal", "Ghana"),
            q("pays", "Dans quel pays est né Joel Embiid ?", "Cameroun", "Nigeria", "Gabon", "Sénégal"),
            q("pays", "Dans quel pays est né Dikembe Mutombo ?", "RD Congo", "Sénégal", "Nigeria", "Cameroun"),
            q("pays", "Dans quel pays est né Pascal Siakam ?", "Cameroun", "Congo", "Nigeria", "Côte d'Ivoire"),
            q("pays", "Dans quel pays est né Tacko Fall ?", "Sénégal", "Mali", "Guinée", "Nigeria"),
            q("pays", "Dans quel pays est né Bismack Biyombo ?", "RD Congo", "Cameroun", "Angola", "Gabon"),
            q("pays", "Pour quel pays joue Giannis Antetokounmpo ?", "Grèce", "Nigeria", "Turquie", "Italie"),
            q("pays", "De quel pays vient Nikola Jokic ?", "Serbie", "Croatie", "Slovénie", "Monténégro"),
            q("pays", "De quel pays vient Luka Doncic ?", "Slovénie", "Serbie", "Croatie", "Lettonie"),
            q("pays", "De quel pays vient Dirk Nowitzki ?", "Allemagne", "Autriche", "Lituanie", "Suisse"),
            q("pays", "De quel pays vient Manu Ginobili ?", "Argentine", "Espagne", "Italie", "Brésil"),
            q("pays", "De quel pays vient Yao Ming ?", "Chine", "Japon", "Corée du Sud", "Philippines"),
            q("pays", "De quel pays vient Pau Gasol ?", "Espagne", "Argentine", "France", "Italie"),
            q("pays", "De quel pays vient Victor Wembanyama ?", "France", "Belgique", "Canada", "Suisse"),
            q("pays", "De quel pays vient Rudy Gobert ?", "France", "Belgique", "Suisse", "Canada"),
            q("pays", "De quel pays vient Kristaps Porzingis ?", "Lettonie", "Lituanie", "Estonie", "Pologne"),
            q("pays", "De quel pays vient Domantas Sabonis ?", "Lituanie", "Lettonie", "Serbie", "Ukraine"),
            q("pays", "De quel pays vient Alperen Sengun ?", "Turquie", "Grèce", "Serbie", "Bulgarie"),
            q("pays", "De quel pays vient Lauri Markkanen ?", "Finlande", "Suède", "Norvège", "Danemark"),
            q("pays", "De quel pays vient Shai Gilgeous-Alexander ?", "Canada", "États-Unis", "Australie", "Bahamas"),
            q("pays", "De quel pays vient Josh Giddey ?", "Australie", "Nouvelle-Zélande", "Canada", "Angleterre"),
            q("pays", "De quel pays vient Franz Wagner ?", "Allemagne", "Autriche", "Pays-Bas", "Suisse"),
            q("pays", "De quel pays vient Drazen Petrovic ?", "Croatie", "Serbie", "Slovénie", "Bosnie"),
            // Une seule équipe, ou la première
            q("carriere", "Avec quelle équipe Larry Bird a-t-il fait toute sa carrière ?", "Celtics", "Lakers", "Pacers", "Knicks"),
            q("carriere", "Avec quelle équipe John Stockton a-t-il fait toute sa carrière ?", "Jazz", "Blazers", "Suns", "Kings"),
            q("carriere", "Avec quelle équipe Tim Duncan a-t-il fait toute sa carrière ?", "Spurs", "Rockets", "Mavericks", "Heat"),
            q("carriere", "Avec quelle équipe Reggie Miller a-t-il fait toute sa carrière ?", "Pacers", "Knicks", "Bulls", "Pistons"),
            q("carriere", "Avec quelle équipe Dirk Nowitzki a-t-il fait toute sa carrière ?", "Mavericks", "Spurs", "Rockets", "Nuggets"),
            q("carriere", "Avec quelle équipe Kobe Bryant a-t-il fait toute sa carrière ?", "Lakers", "Hornets", "Clippers", "76ers"),
            q("carriere", "Avec quelle équipe Stephen Curry a-t-il débuté en NBA ?", "Warriors", "Hornets", "Knicks", "Suns"),
            q("carriere", "Avec quelle équipe LeBron James a-t-il débuté en NBA ?", "Cavaliers", "Heat", "Lakers", "Knicks"),
            q("carriere", "Avec quelle équipe Tony Parker a-t-il gagné ses 4 titres ?", "Spurs", "Hornets", "Lakers", "Heat"),
            q("carriere", "Avec quelle équipe Charles Barkley a-t-il joué les finales 1993 ?", "Suns", "76ers", "Rockets", "Bulls"),
            q("carriere", "Avec quelle équipe Kawhi Leonard a-t-il gagné le titre en 2019 ?", "Raptors", "Spurs", "Clippers", "Warriors"),
            q("carriere", "Qui a fait équipe avec Dwyane Wade et Chris Bosh au Heat ?", "LeBron James", "Kobe Bryant", "Carmelo Anthony", "Paul Pierce"),
            q("carriere", "Quel joueur a inspiré le logo de la NBA ?", "Jerry West", "Michael Jordan", "Bill Russell", "Bob Cousy"),
            q("carriere", "Quel est le vrai prénom de Magic Johnson ?", "Earvin", "Michael", "Marcus", "Ervin"),
            q("carriere", "Combien de titres Michael Jordan a-t-il gagnés ?", "6", "5", "4", "7"),
            q("carriere", "Combien de titres Tim Duncan a-t-il gagnés ?", "5", "4", "6", "3"),
            q("carriere", "Combien de titres Kobe Bryant a-t-il gagnés ?", "5", "4", "6", "3"),
            q("carriere", "Combien de titres Scottie Pippen a-t-il gagnés ?", "6", "5", "4", "3"),
            q("carriere", "Combien de titres Hakeem Olajuwon a-t-il gagnés ?", "2", "1", "3", "4"),
            q("carriere", "Combien de titres Shaquille O'Neal a-t-il gagnés ?", "4", "3", "5", "2"),
            q("carriere", "Combien de titres Magic Johnson a-t-il gagnés ?", "5", "4", "6", "3"),
            q("carriere", "Combien de titres Larry Bird a-t-il gagnés ?", "3", "2", "4", "5"),
            q("carriere", "Combien de titres Dennis Rodman a-t-il gagnés ?", "5", "3", "4", "6"),
            q("carriere", "Avec quelle équipe Michael Jordan a-t-il fini sa carrière ?", "Wizards", "Bulls", "Hornets", "Knicks"),
            q("carriere", "Quelle équipe Allen Iverson a-t-il menée en finale en 2001 ?", "76ers", "Nuggets", "Pistons", "Grizzlies"),
            q("carriere", "Contre quelle équipe Jordan gagne-t-il son dernier titre en 1998 ?", "Jazz", "Suns", "SuperSonics", "Blazers"),
            q("carriere", "Quelle est la première équipe non américaine championne NBA ?", "Raptors", "Grizzlies", "Nuggets", "Suns"),
            q("carriere", "En quelle année LeBron James revient-il à Cleveland ?", "2014", "2012", "2016", "2010"),
            // Surnoms
            q("surnoms", "Qui surnomme-t-on « The Greek Freak » ?", "Giannis Antetokounmpo", "Nikola Vucevic", "Kristaps Porzingis", "Rudy Gobert"),
            q("surnoms", "Qui surnommait-on « The Answer » ?", "Allen Iverson", "Tracy McGrady", "Vince Carter", "Steve Francis"),
            q("surnoms", "Qui surnommait-on « Black Mamba » ?", "Kobe Bryant", "Michael Jordan", "Paul Pierce", "Dwyane Wade"),
            q("surnoms", "Qui surnomme-t-on « The Beard » ?", "James Harden", "Stephen Curry", "Russell Westbrook", "Damian Lillard"),
            q("surnoms", "Qui surnomme-t-on « The Joker » ?", "Nikola Jokic", "Luka Doncic", "Joel Embiid", "Kyrie Irving"),
            q("surnoms", "Qui surnommait-on « The Dream » ?", "Hakeem Olajuwon", "David Robinson", "Patrick Ewing", "Clyde Drexler"),
            q("surnoms", "Qui surnommait-on « The Mailman » ?", "Karl Malone", "John Stockton", "Charles Barkley", "Dennis Rodman"),
            q("surnoms", "Qui surnommait-on « The Admiral » ?", "David Robinson", "Shaquille O'Neal", "Tim Duncan", "Alonzo Mourning"),
            q("surnoms", "Qui surnommait-on « The Diesel » ?", "Shaquille O'Neal", "Karl Malone", "Dwight Howard", "Charles Barkley"),
            q("surnoms", "Qui surnomme-t-on « Slim Reaper » ?", "Kevin Durant", "Anthony Davis", "Chet Holmgren", "Kawhi Leonard"),
            q("surnoms", "Qui surnommait-on « The Human Highlight Film » ?", "Dominique Wilkins", "Vince Carter", "Julius Erving", "Michael Jordan"),
            q("surnoms", "Qui surnommait-on « Dr. J » ?", "Julius Erving", "Earvin Johnson", "Jerry West", "Moses Malone"),
            q("surnoms", "Qui surnommait-on « The Big Fundamental » ?", "Tim Duncan", "Kevin Garnett", "David Robinson", "Pau Gasol"),
            q("surnoms", "Qui surnommait-on « The Glove » ?", "Gary Payton", "John Stockton", "Jason Kidd", "Reggie Miller"),
            q("surnoms", "Qui surnommait-on « The Worm » ?", "Dennis Rodman", "Scottie Pippen", "Horace Grant", "Ben Wallace"),
            q("surnoms", "Qui surnommait-on « Flash » ?", "Dwyane Wade", "Derrick Rose", "Allen Iverson", "Tony Parker"),
            q("surnoms", "Qui surnommait-on « Agent Zero » ?", "Gilbert Arenas", "Paul Pierce", "Vince Carter", "Baron Davis"),
            // Numéros
            q("numeros", "Quel numéro portait Michael Jordan aux Bulls, la plupart du temps ?", "23", "45", "33", "32"),
            q("numeros", "Quels numéros Kobe Bryant a-t-il portés aux Lakers ?", "8 et 24", "8 et 23", "24 et 33", "10 et 24"),
            q("numeros", "Quel numéro portait Larry Bird ?", "33", "32", "34", "23"),
            q("numeros", "Quel numéro portait Magic Johnson ?", "32", "33", "23", "8"),
            q("numeros", "Quel numéro portait Tim Duncan ?", "21", "50", "12", "34"),
            q("numeros", "Quel numéro portait Dirk Nowitzki ?", "41", "14", "31", "44"),
            q("numeros", "Quel numéro portait Allen Iverson aux 76ers ?", "3", "1", "11", "23"),
            q("numeros", "Quel numéro portait Bill Russell ?", "6", "33", "13", "22"),
            q("numeros", "Quel numéro portait Hakeem Olajuwon ?", "34", "33", "32", "50"),
            q("numeros", "Quel numéro portait John Stockton ?", "12", "32", "8", "1"),
            // Anciens noms
            q("franchises", "Sous quel nom jouait le Thunder à Seattle ?", "SuperSonics", "Thunderbirds", "Mariners", "Storm"),
            q("franchises", "Avant Memphis, où jouaient les Grizzlies ?", "Vancouver", "Seattle", "Saint-Louis", "Kansas City"),
            q("franchises", "Avant Brooklyn, où jouaient les Nets ?", "New Jersey", "Boston", "Philadelphie", "Baltimore"),
            q("franchises", "Avant Sacramento, où jouaient les Kings ?", "Kansas City", "Seattle", "San Diego", "Las Vegas"),
            q("franchises", "Avant Utah, où jouait le Jazz ?", "La Nouvelle-Orléans", "Memphis", "Chicago", "Kansas City"),
            q("franchises", "Avant Los Angeles, où jouaient les Lakers ?", "Minneapolis", "San Diego", "Chicago", "Détroit"),
            q("franchises", "Avant Los Angeles, où jouaient les Clippers ?", "San Diego", "Oakland", "Seattle", "Las Vegas"),
            q("franchises", "Avant Houston, où jouaient les Rockets ?", "San Diego", "Dallas", "Denver", "Saint-Louis"),
            q("franchises", "Avant Atlanta, où jouaient les Hawks ?", "Saint-Louis", "Memphis", "Charlotte", "Cincinnati"),
            q("franchises", "Avant 1997, comment s'appelaient les Wizards ?", "Bullets", "Senators", "Generals", "Capitals"),
            q("franchises", "Avant 2013, comment s'appelaient les Pelicans ?", "Hornets", "Jazz", "Saints", "Bucks"),
            q("franchises", "Comment s'appelaient les 76ers à Syracuse ?", "Nationals", "Royals", "Packers", "Warriors"),
            q("franchises", "Où jouent les Nuggets ?", "Denver", "Salt Lake City", "Phoenix", "Portland"),
            q("franchises", "Où jouent les Bucks ?", "Milwaukee", "Cleveland", "Indianapolis", "Détroit"),
            q("franchises", "Où jouent les Spurs ?", "San Antonio", "Dallas", "Houston", "La Nouvelle-Orléans"),
            q("franchises", "Où jouent les Timberwolves ?", "Minneapolis", "Milwaukee", "Chicago", "Denver"),
            q("franchises", "Où jouent les Pacers ?", "Indianapolis", "Cleveland", "Détroit", "Charlotte"),
            q("franchises", "Où jouent les Trail Blazers ?", "Portland", "Seattle", "Sacramento", "Salt Lake City"),
            q("franchises", "Où joue le Magic ?", "Orlando", "Miami", "Atlanta", "Charlotte"),
            q("franchises", "Où joue le Jazz ?", "Salt Lake City", "Denver", "La Nouvelle-Orléans", "Phoenix"),
            // Règles
            q("regles", "Combien de minutes dure un quart-temps en NBA ?", "12", "10", "15", "20"),
            q("regles", "Combien de minutes dure une prolongation en NBA ?", "5", "3", "6", "10"),
            q("regles", "Combien de secondes pour tirer sur une attaque en NBA ?", "24", "30", "20", "14"),
            q("regles", "Combien de fautes personnelles avant l'exclusion en NBA ?", "6", "5", "4", "7"),
            q("regles", "À quelle hauteur se trouve le panier ?", "3,05 m", "2,90 m", "3,15 m", "3,25 m"),
            q("regles", "À quelle distance se tire un lancer franc ?", "4,57 m", "4,00 m", "5,00 m", "3,60 m"),
            q("regles", "Depuis quelle année le tir à 3 points existe-t-il en NBA ?", "1979", "1967", "1985", "1992"),
            q("regles", "Combien de matchs joue une équipe en saison régulière ?", "82", "72", "90", "100"),
            q("regles", "Combien d'équipes compte la NBA ?", "30", "28", "32", "36"),
            q("regles", "Une série de playoffs NBA se joue au meilleur de combien de matchs ?", "7", "5", "3", "9"),
            q("regles", "Combien de joueurs par équipe sur le terrain ?", "5", "6", "4", "7"),
            // Concours de dunks
            q("dunk", "Qui gagne le concours de dunks 2000 à Oakland ?", "Vince Carter", "Tracy McGrady", "Kobe Bryant", "Steve Francis"),
            q("dunk", "Qui gagne le concours de dunks 1986, du haut de ses 1,70 m ?", "Spud Webb", "Muggsy Bogues", "Nate Robinson", "Earl Boykins"),
            q("dunk", "Qui gagne les concours de dunks 2015 et 2016 ?", "Zach LaVine", "Aaron Gordon", "Blake Griffin", "Donovan Mitchell"),
            q("dunk", "Qui gagne trois concours de dunks de suite, de 2023 à 2025 ?", "Mac McClung", "Jaylen Brown", "Zach LaVine", "Obi Toppin"));

    // Un ordre fixe mais mélangé, pour ne pas enchaîner « Champion en 2008, en 2011… ».
    private static final List<Item> SHUFFLED = shuffled();

    private QuizBank() {}

    private static List<Item> shuffled() {
        List<Item> items = new ArrayList<>(ITEMS);
        Collections.shuffle(items, new Random(2026));
        return List.copyOf(items);
    }

    /**
     * Les questions du jour, de thèmes différents. `used` : ce qui est déjà sorti cette
     * saison. Si la liste est épuisée, on en reprend plutôt que de laisser un quiz incomplet.
     */
    static List<Item> pick(LocalDate day, int count, Set<String> used) {
        int start = (int) Math.floorMod(day.toEpochDay() * 2, (long) SHUFFLED.size());
        List<Item> picked = new ArrayList<>();
        Set<String> themes = new HashSet<>();
        for (boolean allowUsed : new boolean[]{false, true}) {
            for (int i = 0; i < SHUFFLED.size() && picked.size() < count; i++) {
                Item item = SHUFFLED.get((start + i) % SHUFFLED.size());
                if ((!allowUsed && used.contains(item.text())) || themes.contains(item.theme()) || picked.contains(item)) continue;
                picked.add(item);
                themes.add(item.theme());
            }
        }
        return picked;
    }
}
