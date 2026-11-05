package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Joueur masqué par un autre dans le fil. Clé « bloqueur:bloqué ». L'autre n'en sait rien. */
@Entity
@Table(indexes = @Index(columnList = "blocker_id"))
@Getter
@Setter
public class UserBlock {

    @Id
    private String id;

    @Column(name = "blocker_id")
    private String blockerId;

    private String blockedId;
}
