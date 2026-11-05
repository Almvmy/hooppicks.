package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Une question du quiz d'un jour. Figée en base : tout le monde a les mêmes, même si les stats bougent dans la journée. */
@Entity
@Table(indexes = @Index(columnList = "day"))
@Getter
@Setter
public class QuizQuestion {

    /** « jour:rang ». */
    @Id
    private String id;

    @Column(name = "day")
    private LocalDate day;

    private int position;

    @Column(length = 200)
    private String text;

    /** Photo du joueur pour « Qui est ce joueur ? ». */
    @Column(length = 500)
    private String imageUrl;

    /** Réponses proposées, une par ligne. */
    @Column(length = 400)
    private String choices;

    private int correct;
}
