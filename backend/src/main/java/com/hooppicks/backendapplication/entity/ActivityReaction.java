package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Réaction rapide (emoji) sur un item du fil d'activité d'une ligue (cf.
 * LeagueService.getRecentActivity). La cible n'est pas un item de fil en
 * tant que tel : le fil est reconstruit à la volée, pas persisté : mais
 * l'entité sous-jacente qu'il représente : targetType="BET" + targetId =
 * Bet.id pour un pari placé ou gagné, targetType="MEMBERSHIP" + targetId =
 * LeagueMembership.id pour une adhésion. Volontairement pas de FK vers Bet/
 * LeagueMembership : une seule table de réactions pour les deux types plutôt
 * que dupliquer le mécanisme.
 */
@Entity
@Getter
@Setter
public class ActivityReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String targetType; // "BET" | "MEMBERSHIP"
    private String targetId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String emoji;
    private Instant createdAt = Instant.now();
}
