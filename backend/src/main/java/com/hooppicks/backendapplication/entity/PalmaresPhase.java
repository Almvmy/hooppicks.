package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Phase (« 2026-27:CUP ») dont les titres ont déjà été attribués : jamais deux fois. */
@Entity
@Getter
@Setter
public class PalmaresPhase {

    @Id
    private String id;

    private Instant awardedAt;
}
