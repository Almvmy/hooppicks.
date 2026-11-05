package com.hooppicks.backendapplication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Signalement d'un message du fil. Clé « joueur:message » : un joueur ne compte qu'une fois. */
@Entity
@Table(indexes = @Index(columnList = "message_id"))
@Getter
@Setter
public class MessageReport {

    @Id
    private String id;

    @Column(name = "message_id")
    private String messageId;

    private String reporterId;

    private Instant createdAt;
}
