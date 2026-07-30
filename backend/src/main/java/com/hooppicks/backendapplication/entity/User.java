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
}