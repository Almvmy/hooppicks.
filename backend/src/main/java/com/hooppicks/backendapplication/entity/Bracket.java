package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Un tableau à remplir (Coupe NBA, plus tard les playoffs). `teams` et `winners`
 * sont des listes de sigles séparés par des virgules, dans l'ordre de BracketTree.
 */
@Entity
@Getter
@Setter
public class Bracket {

    /** « cup-2026 ». */
    @Id
    private String id;

    /** « Coupe NBA 2026 » : apparaît aussi au palmarès. */
    private String label;

    private String teams;

    /** Un vainqueur par match, vide tant qu'il n'est pas connu. */
    private String winners;

    private Instant locksAt;

    private Instant createdAt;

    private Boolean reminderSent;

    private Boolean awarded;
}
