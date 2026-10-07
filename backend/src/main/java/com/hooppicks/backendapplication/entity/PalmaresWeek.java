package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/** Semaine de jeu dont les titres ont déjà été attribués : jamais deux fois. */
@Entity
@Getter
@Setter
public class PalmaresWeek {

    @Id
    private java.time.LocalDate week;
}
