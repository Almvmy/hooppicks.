package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Match boosté en plus du choc de la soirée, ajouté par un admin pour une
 * soirée spéciale (matchs de Noël, phase finale de la Coupe…).
 */
@Entity
@Getter
@Setter
public class ExtraBoost {

    @Id
    private String matchId;

    /** Jour GMT où commence la soirée (à 12h), comme `DailyBoost`. */
    private LocalDate night;
}
