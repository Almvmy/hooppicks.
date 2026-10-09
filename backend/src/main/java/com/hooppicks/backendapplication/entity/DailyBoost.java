package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Match à la cote boostée d'une soirée NBA (12h GMT → 12h GMT le lendemain),
 * figé dès qu'il est choisi : la clé primaire sur la soirée empêche deux choix
 * concurrents.
 */
@Entity
@Getter
@Setter
public class DailyBoost {

    /** Jour GMT où commence la soirée (à 12h). */
    @Id
    private LocalDate night;

    private String matchId;
}
