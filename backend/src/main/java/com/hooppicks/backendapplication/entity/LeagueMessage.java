package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Message du fil d'une ligue : visible des seuls membres, supprimé avec la ligue ou le compte de son auteur. */
@Entity
@Table(indexes = @Index(columnList = "league_id, created_at"))
@Getter
@Setter
public class LeagueMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "league_id")
    private String leagueId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 280)
    private String text;

    @Column(name = "created_at")
    private Instant createdAt;

    /** Le message auquel celui-ci répond. Sans clé étrangère : il peut disparaître, la réponse reste. */
    @Column(name = "reply_to_id")
    private String replyToId;
}
