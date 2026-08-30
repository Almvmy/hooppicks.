package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private Long externalId;

    private String firstName;
    private String lastName;
    private String position; // ex: "G", "F", "C" (tel que renvoyé par balldontlie, pas toujours normalisé)
    private String height;   // ex: "6-6" (pieds-pouces, format brut de l'API)
    private String weight;   // ex: "220" (livres)

    // LAZY + @BatchSize sur Team (cf. Team.java) : même correctif N+1 que
    // Match.homeTeam/awayTeam et RosterPlayer.team.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
