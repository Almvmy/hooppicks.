package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** La partie d'un joueur pour un jour. Clé « joueur:jour » : un seul essai. */
@Entity
@Table(indexes = @Index(columnList = "week"))
@Getter
@Setter
public class QuizAttempt {

    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "day")
    private LocalDate day;

    /** Lundi de la semaine de jeu. */
    private LocalDate week;

    /** Un caractère par question jouée : « 1 » panier, « 0 » raté. Sa longueur donne la question en cours. */
    @Column(length = 10)
    private String shots;

    /** Moment où le chrono de la question en cours démarre. */
    private Instant questionStartedAt;

    /** Temps total de réflexion : départage les égalités. */
    private long totalMillis;
}
