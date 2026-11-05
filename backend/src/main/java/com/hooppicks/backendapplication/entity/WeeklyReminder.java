package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Rappel de la semaine déjà envoyé (« 2026-11-02:SUNDAY ») : noté en base pour
 * ne jamais repartir après un redémarrage.
 */
@Entity
@Getter
@Setter
public class WeeklyReminder {

    @Id
    private String id;

    private Instant sentAt;
}
