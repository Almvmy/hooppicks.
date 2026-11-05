package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Mission de la semaine réussie. Clé « joueur:semaine:mission » : enregistrer
 * deux fois la même (deux onglets ouverts) ne compte qu'une fois.
 */
@Entity
@Table(indexes = @Index(columnList = "user_id"))
@Getter
@Setter
public class MissionCompletion {

    @Id
    private String id;

    @jakarta.persistence.Column(name = "user_id")
    private String userId;

    private Instant completedAt;
}
