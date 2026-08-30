package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    private String id; // ex: "lal"

    private String name;
    private String abbreviation;
    private String conference; // "Est" ou "Ouest"
    private String division;

    // Force de l'équipe pour le calcul des cotes (cf. OddsService/EloService).
    // Défaut 1500 = valeur de départ Elo standard, backfillée par Postgres via
    // le columnDefinition pour les équipes déjà en base.
    @Column(columnDefinition = "double precision default 1500.0")
    private double eloRating = 1500.0;

    // Vrai classement (victoires/défaites officielles), importé depuis ESPN
    // (cf. EspnStandingsService) : purement informatif, distinct de l'Elo qui
    // reste la seule donnée utilisée pour le calcul des cotes (cf. OddsService).
    // Tous nullable : pas encore synchronisé tant que EspnStandingsService n'a
    // pas tourné une première fois.
    private Integer wins;
    private Integer losses;
    private String streak; // ex. "W3", "L1"
    private Integer conferenceSeed;
    private String gamesBehind; // ex. "2.5", ou "-" pour le premier de la conférence

    // Logo officiel (CDN ESPN, cf. EspnStandingsService) : même source que
    // les photos de joueurs (RosterPlayer.headshotUrl) : image statique
    // publique, pas soumise au filtre Akamai des endpoints JSON.
    private String logoUrl;

    // Nombre de joueurs au statut "Out" dans l'effectif (cf. RosterPlayer.
    // injuryStatus), recalculé à chaque synchro roster (EspnRosterService).
    // Dénormalisé exprès, comme wins/streak : sert à afficher un badge
    // blessure sur les cartes de match sans requête supplémentaire par
    // équipe à chaque affichage de la liste des matchs. Ne compte que "Out"
    // (indisponibilité certaine), pas "Day-To-Day" (trop incertain pour être
    // un signal fiable côté pari).
    private Integer outPlayersCount;
}