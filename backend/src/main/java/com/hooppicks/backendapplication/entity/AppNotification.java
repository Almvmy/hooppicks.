package com.hooppicks.backendapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // 1000 : une annonce de la console fait jusqu'à 280 caractères (agrandie au démarrage par ColumnWidening).
    @Column(length = 1000)
    private String message;

    /** Page où mène la notification, quand son type ne suffit pas à le dire (une ligue précise…). */
    private String link;

    private Instant date = Instant.now();
    private boolean read = false;
}
