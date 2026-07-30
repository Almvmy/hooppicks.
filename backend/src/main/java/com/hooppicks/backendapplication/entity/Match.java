package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    private Instant date;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private Integer homeScore;
    private Integer awayScore;

    @Column(unique = true)
    private Long externalId;

    // Cotes fixes, stockées à plat directement sur le match (cf. décision "cotes fixes" prise avec ton ami)
    private double moneylineHome;
    private double moneylineAway;
    private double spreadValue;
    private double spreadOddsHome;
    private double spreadOddsAway;
    private double totalValue;
    private double totalOddsOver;
    private double totalOddsUnder;
}