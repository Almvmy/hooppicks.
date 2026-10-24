package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Dernier passage d'une tâche de fond (cf. JobMonitor) : gardé en base pour survivre aux redéploiements. */
@Entity
@Getter
@Setter
public class JobRun {

    @Id
    private String name;

    private Instant lastSuccessAt;
    private Instant lastFailureAt;

    @Column(length = 500)
    private String lastError;

    // Résultat du dernier passage réussi (« 0 joueur mis à jour sur 8 ») :
    // une tâche qui « réussit » sans rien faire est aussi un signal.
    @Column(length = 255)
    private String lastDetail;
}
