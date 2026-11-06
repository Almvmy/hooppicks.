package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.bet.PlayerPropsService;

import java.util.HashMap;
import java.util.Map;

/**
 * La photo d'un camp du débat, quand le camp est un joueur ou une équipe. Liste écrite
 * à la main et non cherchée dans les effectifs : « Anthony » y trouverait Cole Anthony
 * et non Carmelo, et une mauvaise photo est pire que pas de photo. Identifiants ESPN
 * vérifiés un à un (nom et portrait) en novembre 2026. Les années 90 n'ont pas de
 * portrait chez ESPN (Jordan, Magic, Bird, Olajuwon…) : pas de photo pour eux.
 */
final class DebateFaces {

    private static final String PORTRAIT = "https://a.espncdn.com/i/headshots/nba/players/full/%d.png";
    private static final String LOGO = "https://a.espncdn.com/i/teamlogos/nba/500/%s.png";

    private static final Map<String, String> IMAGES = new HashMap<>();

    private static void player(long espnId, String... names) {
        for (String name : names) IMAGES.put(PlayerPropsService.normalize(name), PORTRAIT.formatted(espnId));
    }

    private static void team(String slug, String... names) {
        for (String name : names) IMAGES.put(PlayerPropsService.normalize(name), LOGO.formatted(slug));
    }

    static {
        player(1966, "LeBron", "LeBron James");
        player(3975, "Curry", "Stephen Curry", "Steph Curry");
        player(3202, "Durant", "Kevin Durant");
        player(3112335, "Jokic", "Nikola Jokic");
        player(5104157, "Wembanyama", "Victor Wembanyama", "Wemby");
        player(3945274, "Doncic", "Luka Doncic");
        player(3032977, "Giannis", "Giannis Antetokounmpo", "Antetokounmpo");
        player(3059318, "Embiid", "Joel Embiid");
        player(4278073, "Gilgeous-Alexander", "Shai Gilgeous-Alexander", "SGA");
        player(4594268, "Edwards", "Anthony Edwards");
        player(4065648, "Tatum", "Jayson Tatum");
        player(6442, "Irving", "Kyrie Irving");
        player(3992, "Harden", "James Harden");
        player(6606, "Lillard", "Damian Lillard");
        player(4279888, "Morant", "Ja Morant");
        player(4396993, "Haliburton", "Tyrese Haliburton");
        player(4433255, "Holmgren", "Chet Holmgren");
        player(3032976, "Gobert", "Rudy Gobert");
        player(3456, "Rose", "Derrick Rose");
        player(110, "Kobe", "Kobe Bryant");
        player(614, "Shaq", "Shaquille O'Neal");
        player(215, "Duncan", "Tim Duncan");
        player(261, "Garnett", "Kevin Garnett");
        player(1987, "Wade", "Dwyane Wade");
        player(1975, "Anthony", "Carmelo Anthony");
        player(592, "Nash", "Steve Nash");
        player(609, "Nowitzki", "Dirk Nowitzki");
        player(9, "Ray Allen");
        player(136, "Vince Carter");
        player(1015, "Tony Parker");

        // Pas d'entrée « Magic » : dans les débats, c'est Magic Johnson.
        team("atl", "Hawks");
        team("bos", "Celtics");
        team("bkn", "Nets");
        team("cha", "Hornets");
        team("chi", "Bulls");
        team("cle", "Cavaliers", "Cavs");
        team("dal", "Mavericks", "Mavs");
        team("den", "Nuggets");
        team("det", "Pistons");
        team("gs", "Warriors");
        team("hou", "Rockets");
        team("ind", "Pacers");
        team("lac", "Clippers");
        team("lal", "Lakers");
        team("mem", "Grizzlies");
        team("mia", "Heat");
        team("mil", "Bucks");
        team("min", "Timberwolves", "Wolves");
        team("no", "Pelicans");
        team("ny", "Knicks");
        team("okc", "Thunder");
        team("phi", "76ers", "Sixers");
        team("phx", "Suns");
        team("por", "Trail Blazers", "Blazers");
        team("sac", "Kings");
        team("sa", "Spurs");
        team("tor", "Raptors");
        team("utah", "Jazz");
        team("wsh", "Wizards");
    }

    private DebateFaces() {}

    /** Null si le camp n'est ni un joueur ni une équipe de la liste (« Oui », « Le sûr »…). */
    static String of(String option) {
        return option == null ? null : IMAGES.get(PlayerPropsService.normalize(option));
    }
}
