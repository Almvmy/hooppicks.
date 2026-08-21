package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "bet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BetSelection> selections = new ArrayList<>();

    private int stake;
    private double totalOdds;
    private int potentialPayout;

    @Enumerated(EnumType.STRING)
    private BetStatus status = BetStatus.PENDING;

    private Instant placedAt = Instant.now();
    private Instant resolvedAt;
}