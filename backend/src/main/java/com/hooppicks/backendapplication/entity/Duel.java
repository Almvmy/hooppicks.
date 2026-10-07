package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Duel 1 contre 1 sur une semaine de jeu : celui qui marque le plus de
 * points de classement cette semaine-là gagne. Pas de mise : l'enjeu est
 * l'honneur (et le palmarès). Les points sont figés à la clôture (FINISHED)
 * pour que le résultat ne bouge plus si un pari est corrigé après coup.
 */
@Entity
@Table(indexes = {
        @Index(columnList = "challenger_id"),
        @Index(columnList = "opponent_id"),
        @Index(columnList = "status, week")
})
@Getter
@Setter
public class Duel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "challenger_id")
    private User challenger;

    @ManyToOne
    @JoinColumn(name = "opponent_id")
    private User opponent;

    /** Lundi de la semaine de jeu disputée (cf. BankrollService.weekOf). */
    private LocalDate week;

    @Enumerated(EnumType.STRING)
    private DuelStatus status = DuelStatus.PENDING;

    private Long challengerPoints;
    private Long opponentPoints;
    /** Null en cas d'égalité. */
    private String winnerId;

    private Instant createdAt = Instant.now();
    private Instant respondedAt;
}
