package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Message du fil de la soirée NBA : un seul fil par soirée (12h → 12h GMT),
 * avec le match en étiquette quand il est écrit depuis sa page. Pas un fil par
 * match : une vingtaine de joueurs répartis sur 12 matchs, c'était 12 salles vides.
 * `user` null : message automatique (fin de quart-temps, grosse perf, gros gain).
 */
@Entity
@Table(indexes = {@Index(columnList = "night, created_at"), @Index(columnList = "match_id, created_at")})
@Getter
@Setter
public class NightMessage {

    /** UUID, ou « auto:… » pour un message automatique : ne part jamais deux fois. */
    @Id
    private String id;

    private LocalDate night;

    @Column(name = "match_id")
    private String matchId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 280)
    private String text;

    @Column(name = "created_at")
    private Instant createdAt;

    /** Argument du débat de ce jour : hors du fil de la soirée, mêmes règles et même modération. */
    @Column(name = "debate_day")
    private LocalDate debateDay;

    /** Le message auquel celui-ci répond. Sans clé étrangère : il peut disparaître, la réponse reste. */
    @Column(name = "reply_to_id")
    private String replyToId;

    /** Masqué : 3 signalements, en attente de la décision d'un admin. */
    private Boolean hidden;

    /** Rétabli par un admin : les nouveaux signalements ne le masquent plus. */
    private Boolean reviewed;
}
