package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Le tableau d'un joueur : un vainqueur choisi par match. Clé « joueur:tableau ». */
@Entity
@Table(indexes = @Index(columnList = "bracket_id"))
@Getter
@Setter
public class BracketPick {

    @Id
    private String id;

    @Column(name = "bracket_id")
    private String bracketId;

    @Column(name = "user_id")
    private String userId;

    private String picks;
}
