package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Pronostic de saison d'un joueur (« qui sera champion NBA ? ») : une équipe
 * par question et par saison, modifiable jusqu'au premier match de saison
 * régulière (SeasonPickService). Question et saison en texte : la liste des
 * questions vit dans le code, pas besoin d'enum figé en base.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "season", "question"}))
@Getter
@Setter
public class SeasonPick {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id")
    private String userId;

    /** « 2026-27 ». */
    private String season;
    private String question;
    private String teamAbbreviation;

    private Instant updatedAt = Instant.now();
}
