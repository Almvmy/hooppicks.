package com.hooppicks.backendapplication.admin;

/** Action admin refusée : message affiché tel quel dans la console, statut HTTP à renvoyer. */
public class AdminActionException extends RuntimeException {

    private final int status;

    public AdminActionException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
