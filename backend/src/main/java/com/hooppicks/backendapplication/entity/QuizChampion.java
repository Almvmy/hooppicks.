package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Le champion du quiz d'une semaine finie. Sa présence marque aussi la semaine comme déjà traitée. */
@Entity
@Getter
@Setter
public class QuizChampion {

    @Id
    private LocalDate week;

    private String userId;

    private String username;

    private int baskets;
}
