package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Bonne réponse à une question de saison, saisie par un admin (console). */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"season", "question"}))
@Getter
@Setter
public class SeasonResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String season;
    private String question;
    private String teamAbbreviation;
    private Instant decidedAt = Instant.now();
}
