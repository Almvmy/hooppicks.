package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Message d'un joueur à l'équipe (bug, question, idée…), envoyé depuis
 * l'app. La réponse de l'admin est gardée ici et part dans sa cloche : tout
 * l'échange reste dans l'app, sans dépendre de l'envoi d'e-mails.
 * Catégorie et statut en texte, pas en enum : Hibernate figerait leurs
 * valeurs dans une contrainte CHECK (cf. EnumCheckConstraintCleanup).
 */
@Entity
@Getter
@Setter
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String category;

    @Column(length = 2000)
    private String message;

    /** Page où était le joueur et son appareil : de quoi reproduire un bug sans lui poser de questions. */
    private String page;
    @Column(length = 400)
    private String device;
    private Boolean appInstalled;

    private Instant createdAt = Instant.now();

    /** OPEN (à traiter), ANSWERED (réponse envoyée), CLOSED (traité sans réponse). */
    private String status = "OPEN";

    @Column(length = 2000)
    private String reply;
    private Instant repliedAt;
    /** Pseudo copié : la réponse garde son auteur même si son compte disparaît. */
    private String repliedBy;
}
