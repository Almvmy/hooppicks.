package com.hooppicks.backendapplication.push;

import com.hooppicks.backendapplication.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Un appareil (navigateur) abonné aux notifications push. Un utilisateur
 * peut en avoir plusieurs (téléphone + ordinateur), d'où une entité à part
 * plutôt qu'un champ sur User.
 */
@Entity
@Getter
@Setter
@Table(indexes = @Index(columnList = "user_id"))
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // URL du service push du navigateur : unique par appareil, c'est elle
    // qui identifie l'abonnement.
    @Column(nullable = false, unique = true, length = 1024)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    private Instant createdAt = Instant.now();
}
