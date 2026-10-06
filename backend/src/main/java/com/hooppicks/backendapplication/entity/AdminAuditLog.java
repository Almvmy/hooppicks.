package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Trace d'une action faite depuis la console admin (/console/**). Sans rôle
 * « super-admin », n'importe quel admin peut en rétrograder un autre ou
 * supprimer un compte : ce journal dit qui a fait quoi, et quand. Le pseudo
 * est copié (pas une clé étrangère) : la trace survit à la suppression ou au
 * renommage du compte admin.
 */
@Entity
@Getter
@Setter
@Table(indexes = @Index(columnList = "date"))
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String adminId;
    private String adminUsername;

    /** Code stable (SYNC_GAMES, VOID_BET, ADJUST_WALLET…), traduit côté frontend. */
    private String action;

    /** Ce sur quoi porte l'action, lisible : pseudo, affiche du match… */
    private String target;

    @Column(length = 500)
    private String details;

    private Instant date = Instant.now();
}
