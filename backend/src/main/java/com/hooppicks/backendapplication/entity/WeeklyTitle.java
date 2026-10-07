package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Titre de « champion de la semaine » : meilleur score d'une semaine de jeu,
 * sur tout HoopPicks (leagueId null) ou dans une ligue. Gardé à vie : c'est
 * ce qui donne de la valeur aux semaines passées, que le classement de la
 * semaine oublie dès le lundi. Noms copiés (pas de clé étrangère vers la
 * ligue) : le titre survit à la suppression de la ligue.
 */
@Entity
@Table(indexes = {@Index(columnList = "user_id"), @Index(columnList = "week")})
@Getter
@Setter
public class WeeklyTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id")
    private String userId;

    private LocalDate week;

    /** Null : champion de tout HoopPicks. */
    private String leagueId;
    private String leagueName;

    private long points;
}
