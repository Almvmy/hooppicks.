package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/** Mot refusé dans le fil, écrit en minuscules sans accents. La liste se tient dans la console. */
@Entity
@Getter
@Setter
public class ChatWord {

    @Id
    private String word;
}
