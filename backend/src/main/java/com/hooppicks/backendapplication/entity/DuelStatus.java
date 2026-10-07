package com.hooppicks.backendapplication.entity;

public enum DuelStatus {
    /** Défi envoyé, en attente de réponse. */
    PENDING,
    /** Accepté : le duel se joue sur la semaine. */
    ACCEPTED,
    DECLINED,
    /** Retiré par celui qui l'avait lancé. */
    CANCELLED,
    /** Semaine finie sans réponse. */
    EXPIRED,
    /** Semaine finie, points figés et vainqueur désigné. */
    FINISHED
}
