package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String username;

    private String passwordHash;

    private String favoriteTeam = "";

    private int walletBalance = 1000; // solde de départ, comme ton mock frontend

    @Column(columnDefinition = "integer default 0", nullable = false)
    private int avatarNumber = 0;

    @Column(columnDefinition = "varchar(255) default 'PG'")
    private String avatarPosition = "PG";

    @Column(columnDefinition = "varchar(255) default 'orange'")
    private String avatarColorway = "orange";

    @Column(columnDefinition = "varchar(255) default 'dunk'")
    private String avatarIcon = "dunk";

    @Column(columnDefinition = "boolean default false")
    private boolean isAdmin = false;

    // Non bloquant : le compte reste utilisable sans vérifier son email (pas
    // d'argent réel en jeu), mais un email non vérifié peut être invalide ou
    // appartenir à quelqu'un d'autre — surtout utile pour fiabiliser le reset
    // de mot de passe, qui en dépend entièrement.
    @Column(columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    @Column(columnDefinition = "boolean default true")
    private boolean notifyMatchStarting = true;

    @Column(columnDefinition = "boolean default true")
    private boolean notifyBetResults = true;

    @Column(columnDefinition = "boolean default true")
    private boolean notifyLeagueActivity = true;

    // columnDefinition sans "not null" : @CreationTimestamp force sinon une
    // contrainte NOT NULL, que Postgres refuse d'ajouter tant que les comptes
    // créés avant ce champ n'ont pas de valeur rétroactive à lui donner.
    @CreationTimestamp
    @Column(columnDefinition = "timestamptz")
    private Instant createdAt;
}