package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Le camp d'un joueur. Clé « joueur:jour » : un seul vote, qu'on peut changer tant que le débat est ouvert. */
@Entity
@Table(indexes = @Index(columnList = "day"))
@Getter
@Setter
public class DebateVote {

    @Id
    private String id;

    @Column(name = "day")
    private LocalDate day;

    @Column(name = "user_id")
    private String userId;

    /** « A » ou « B ». */
    private String side;
}
