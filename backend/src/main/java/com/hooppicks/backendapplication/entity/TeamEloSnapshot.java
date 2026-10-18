package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Elo d'une équipe à midi GMT un jour donné : sert à l'évolution des power rankings. */
@Entity
@Table(indexes = @Index(columnList = "date"))
@Getter
@Setter
public class TeamEloSnapshot {

    /** « 2026-10-20:9 » : un relevé par équipe et par jour. */
    @Id
    private String id;

    private LocalDate date;

    private String teamId;

    private double elo;
}
