package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Session de connexion gardée en base : un redéploiement ne déconnecte plus
 * personne. Seule l'empreinte SHA-256 du jeton est stockée, jamais le jeton
 * lui-même : une fuite de la base ne permet pas d'ouvrir les sessions.
 */
@Entity
@Table(indexes = @Index(columnList = "userId"))
@Getter
@Setter
public class UserSession {

    @Id
    @Column(length = 64)
    private String tokenHash;

    private String userId;
    private Instant createdAt = Instant.now();
    private Instant lastSeenAt = Instant.now();
    private Instant expiresAt;
}
