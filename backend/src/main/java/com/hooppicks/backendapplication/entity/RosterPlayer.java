package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Effectif actuel d'une équipe, importé depuis l'API (non officielle)
 * d'ESPN : remplace l'ancienne approche "chercher un joueur" (cf. l'ancien
 * commentaire dans TeamRoster.tsx) : balldontlie en free tier ne distingue
 * pas les joueurs actifs des retraités, ce qui rendait tout listing brut
 * inutilisable. Id = l'id athlète ESPN directement (stable, unique), pas de
 * lien avec l'entité Player de balldontlie : même raison que PlayerMatchStat.
 */
@Entity
@Getter
@Setter
public class RosterPlayer {

    @Id
    private String id; // id athlète ESPN

    // LAZY + @BatchSize sur Team (cf. Team.java) : voir le commentaire
    // équivalent sur Match.homeTeam/awayTeam, même correctif N+1.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    private String firstName;
    private String lastName;
    private String position; // ex. "G", "F", "C" (peut être vide, ESPN ne le garantit pas pour tous)
    private String jersey; // texte : ESPN ne l'assigne pas toujours (ex. joueur en camp d'entraînement)
    private String height; // ex. "6' 5\""
    private String weight; // ex. "184 lbs"
    private String headshotUrl;

    // Statut blessure : vient de la même réponse ESPN que le reste du roster
    // (pas d'appel séparé). Null si pas blessé. injuryDate = date du dernier
    // statut connu (ESPN ne fournit ni description ni partie du corps).
    private String injuryStatus; // ex. "Day-To-Day", "Out"
    private Instant injuryDate;

    // Moyennes saison (cf. EspnPlayerStatsService) : synchronisées à part du
    // reste du roster, par petits lots (un appel ESPN par joueur, ~550
    // joueurs au total, jamais tous d'un coup). Tous nullable tant que le
    // joueur n'a pas encore été traité. statsUpdatedAt sert à prioriser :
    // jamais synchronisé (null) d'abord, puis le plus ancien.
    private String statsSeasonLabel; // ex. "2025-26"
    private Integer gamesPlayed;
    private Integer gamesStarted;
    private Double minutesPerGame;
    private Double pointsPerGame;
    private Double reboundsPerGame;
    private Double assistsPerGame;
    private Double stealsPerGame;
    private Double blocksPerGame;
    private Double turnoversPerGame;
    private Double fieldGoalPct;
    private Double threePointPct;
    private Double freeThrowPct;
    private Instant statsUpdatedAt;
}
