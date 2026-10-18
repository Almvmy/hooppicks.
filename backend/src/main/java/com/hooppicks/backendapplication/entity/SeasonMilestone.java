package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Étapes des pronostics de saison déjà annoncées (rappel la veille du
 * verrou, révélation des choix) : notées en base pour ne jamais repartir
 * après un redémarrage.
 */
@Entity
@Getter
@Setter
public class SeasonMilestone {

    @Id
    private String season;

    private Instant reminderSentAt;

    private Instant revealedAt;
}
