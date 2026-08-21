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
}