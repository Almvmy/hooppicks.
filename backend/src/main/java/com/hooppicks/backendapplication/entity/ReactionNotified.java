package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Réaction déjà signalée à l'auteur (« joueur:type:cible ») : retirer puis
 * remettre sa réaction ne renvoie pas une notification à chaque fois.
 */
@Entity
@Getter
@Setter
public class ReactionNotified {

    @Id
    private String id;
}
