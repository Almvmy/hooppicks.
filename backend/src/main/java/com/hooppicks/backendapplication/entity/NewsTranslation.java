package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Traduction française d'un texte d'actualité, gardée en base : en mémoire
 * seulement, chaque redéploiement retraduisait tout le fil (quota DeepL), et
 * une panne de DeepL juste après un redémarrage repassait toute la page en anglais.
 */
@Entity
@Getter
@Setter
public class NewsTranslation {

    /** Empreinte SHA-256 du texte anglais : une description peut dépasser la longueur d'une clé. */
    @Id
    private String sourceHash;

    @Column(columnDefinition = "text")
    private String translated;

    private Instant createdAt;
}
