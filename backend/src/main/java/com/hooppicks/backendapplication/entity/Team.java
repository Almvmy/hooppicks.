package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    private String id; // ex: "lal"

    private String name;
    private String abbreviation;
    private String conference; // "Est" ou "Ouest"
    private String division;
}