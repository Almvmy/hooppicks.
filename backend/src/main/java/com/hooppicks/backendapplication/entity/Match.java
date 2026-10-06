package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // LAZY + @BatchSize sur Team (cf. Team.java) : sans ça, Hibernate charge
    // homeTeam/awayTeam par un SELECT séparé et immédiat pour chaque match
    // (comportement par défaut de @ManyToOne), soit jusqu'à 2 requêtes par
    // match affiché — mesuré en charge : 140k+ requêtes "team" quasi
    // identiques pour une table de 30 lignes qui ne change qu'à la synchro.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    private Instant date;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private Integer homeScore;
    private Integer awayScore;

    @Column(unique = true)
    private Long externalId;

    // ID de l'event ESPN correspondant (système d'identifiants totalement
    // différent de balldontlie.io) : résolu par date + sigles d'équipe une
    // fois le match connu, voir EspnStatsService. Nullable : ESPN n'a pas
    // forcément indexé le match au moment de la synchro balldontlie.
    private String espnEventId;

    // Phase (présaison, Coupe NBA, playoffs…), intitulé traduit ("1er tour
    // Est · Match 2") et état de la série en playoffs ("NYK mène 2-0") : lus
    // sur ESPN, voir EspnMatchStage. Null tant que le match n'y est pas relié.
    @Enumerated(EnumType.STRING)
    private MatchType type;
    private String stageLabel;
    private String seriesSummary;

    // Notifications « ton équipe favorite » déjà envoyées pour ce match (une
    // seule fois chacune, cf. FavoriteTeamNotifier). Boolean nullable : une
    // colonne NOT NULL ne s'ajoute pas à une table qui a déjà des lignes.
    private Boolean favoriteKickoffNotified;
    private Boolean favoriteResultNotified;

    // Corrigé à la main depuis la console admin : les synchros (balldontlie,
    // ESPN) ne touchent plus à son statut ni à son score, sinon la
    // correction serait écrasée au tick suivant par la donnée externe fausse
    // qu'elle venait justement rattraper. Levé par « Déverrouiller ».
    private Boolean adminLocked;

    // Cotes fixes, stockées à plat directement sur le match (cf. décision "cotes fixes" prise avec ton ami)
    private double moneylineHome;
    private double moneylineAway;
    private double spreadValue;
    private double spreadOddsHome;
    private double spreadOddsAway;
    private double totalValue;
    private double totalOddsOver;
    private double totalOddsUnder;
}