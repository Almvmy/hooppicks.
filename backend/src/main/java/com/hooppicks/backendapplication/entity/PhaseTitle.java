package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Titre de champion d'une phase de la saison : Coupe NBA (`CUP`) ou playoffs
 * (`PLAYOFFS`), meilleur score du classement de la phase, sur tout HoopPicks
 * (leagueId null) ou dans une ligue. Mêmes règles que `WeeklyTitle` : gardé
 * à vie, nom de ligue copié.
 */
@Entity
@Table(indexes = {@Index(columnList = "user_id"), @Index(columnList = "season, phase")})
@Getter
@Setter
public class PhaseTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id")
    private String userId;

    /** « 2026-27 ». */
    private String season;

    /** CUP ou PLAYOFFS (texte : pas de contrainte CHECK figée). */
    private String phase;

    /** Null : champion de tout HoopPicks. */
    private String leagueId;
    private String leagueName;

    private long points;
}
