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

    // Solde de pari de la semaine de jeu en cours : remis à 1000 chaque lundi
    // à midi (cf. BankrollService). Ne compte jamais au classement.
    private int walletBalance = 1000;

    // Lundi de la semaine de jeu à laquelle appartient walletBalance. Null =
    // compte antérieur aux semaines de jeu : remis à niveau au premier passage.
    private java.time.LocalDate bankrollWeek;

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
    // appartenir à quelqu'un d'autre : surtout utile pour fiabiliser le reset
    // de mot de passe, qui en dépend entièrement.
    @Column(columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    @Column(columnDefinition = "boolean default true")
    private boolean notifyMatchStarting = true;

    @Column(columnDefinition = "boolean default true")
    private boolean notifyBetResults = true;

    @Column(columnDefinition = "boolean default true")
    private boolean notifyLeagueActivity = true;

    // Ton équipe favorite joue dans l'heure / son résultat final.
    @Column(columnDefinition = "boolean default true")
    private boolean notifyFavoriteTeam = true;

    // columnDefinition sans "not null" : @CreationTimestamp force sinon une
    // contrainte NOT NULL, que Postgres refuse d'ajouter tant que les comptes
    // créés avant ce champ n'ont pas de valeur rétroactive à lui donner.
    @CreationTimestamp
    @Column(columnDefinition = "timestamptz")
    private Instant createdAt;
}
