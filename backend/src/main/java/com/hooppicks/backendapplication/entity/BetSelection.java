package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class BetSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "bet_id")
    private Bet bet;

    private String matchId;
    private String matchLabel;
    private String market;  // "moneyline" | "spread" | "total"
    private String outcome; // "home" | "away" | "over" | "under"
    private String label;
    private double odds;

    // Suivi en direct d'un combiné (BetResolutionService.trackProgress) :
    // sélection déjà annoncée comme validée. Boolean nullable : colonne
    // ajoutée à une table déjà remplie.
    private Boolean progressNotified;

    // Pari joueur (market "player_points", PlayerPropsService) : joueur et
    // ligne figés à la pose, la moyenne du joueur bougeant ensuite.
    private String playerId;
    private String playerName;
    private Double propLine;
}
