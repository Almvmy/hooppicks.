package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}