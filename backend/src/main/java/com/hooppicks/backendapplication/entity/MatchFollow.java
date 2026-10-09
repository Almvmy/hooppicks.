package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Un joueur suit un match (cloche sur la page du match) : notifications au
 * coup d'envoi, à chaque fin de quart-temps, en fin de match serrée et au
 * résultat. Ce qui est déjà parti est noté ici, pas en mémoire : un
 * redémarrage du serveur ne renvoie rien deux fois.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "matchId"}))
@Getter
@Setter
public class MatchFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String matchId;
    private Instant createdAt = Instant.now();

    // Drapeaux Boolean nullables : une colonne NOT NULL ne s'ajoute pas à une table déjà remplie.
    private Boolean kickoffNotified;
    /** Dernière période dont la fin a été annoncée (0 = aucune). */
    private Integer lastPeriodNotified;
    private Boolean closeFinishNotified;
    private Boolean finalNotified;
}
