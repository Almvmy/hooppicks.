package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Le débat d'une soirée NBA : une question, deux camps. Créé au premier affichage, ou à l'avance par un admin. */
@Entity
@Getter
@Setter
public class Debate {

    @Id
    private LocalDate day;

    @Column(length = 140)
    private String question;

    @Column(length = 40)
    private String optionA;

    @Column(length = 40)
    private String optionB;

    /** Écrit par un admin, pas tiré de la liste. */
    private Boolean planned;
}
