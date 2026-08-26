package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Feuille de match individuelle d'un joueur, importée depuis l'API (non
 * officielle) d'ESPN — balldontlie ne fournit ces stats détaillées que
 * derrière un tier payant. Le joueur est stocké en texte libre (nom, sigle
 * d'équipe), pas relié à l'entité Player : ESPN a son propre système d'ID
 * athlète, encore différent de balldontlie, et rapprocher les deux de façon
 * fiable serait un chantier à part — pas nécessaire pour afficher une
 * feuille de match lisible.
 */
@Entity
@Getter
@Setter
public class PlayerMatchStat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    private String playerName;
    private String teamAbbreviation;
    private boolean starter;

    private String minutes; // ex. "27" — laissé en texte, ESPN renvoie parfois "--" (DNP)

    private int points;
    private int rebounds;
    private int assists;
    private int steals;
    private int blocks;
    private int turnovers;
    private int plusMinus;

    private int fieldGoalsMade;
    private int fieldGoalsAttempted;
    private int threePointsMade;
    private int threePointsAttempted;
    private int freeThrowsMade;
    private int freeThrowsAttempted;
}
