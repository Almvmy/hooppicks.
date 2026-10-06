package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Rang d'un joueur au classement saison, photographié une fois par jour :
 * sert uniquement à afficher l'évolution (↑3 / ↓1) depuis la veille. Pas de
 * clé étrangère vers User : une photo n'a aucune raison de bloquer la
 * suppression d'un compte, et une ligne orpheline est juste ignorée.
 */
@Entity
@Getter
@Setter
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "snapshotDate"}),
        indexes = @Index(columnList = "snapshotDate")
)
public class RankSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private int rank;
    private LocalDate snapshotDate;
}
