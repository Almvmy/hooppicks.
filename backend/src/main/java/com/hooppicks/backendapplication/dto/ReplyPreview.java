package com.hooppicks.backendapplication.dto;

/**
 * Le message auquel on répond, tel qu'il se lit au moment de l'affichage : jamais
 * copié dans la réponse, sinon un message supprimé ou masqué par la modération
 * resterait lisible dans toutes ses réponses. `text` null : plus visible.
 */
public record ReplyPreview(String id, String username, String text) {

    private static final int MAX_EXCERPT = 80;

    public static ReplyPreview of(String id, String username, String text) {
        String excerpt = text.length() <= MAX_EXCERPT ? text : text.substring(0, MAX_EXCERPT - 1) + "…";
        return new ReplyPreview(id, username, excerpt);
    }

    public static ReplyPreview gone(String id) {
        return new ReplyPreview(id, null, null);
    }
}
