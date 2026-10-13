package com.hooppicks.backendapplication.feedback;

import com.hooppicks.backendapplication.entity.Feedback;

/** Un message et sa réponse. Pseudo, page et appareil ne sont remplis que pour la console admin. */
public record FeedbackDto(
        String id,
        String category,
        String message,
        String createdAt,
        String status,
        String reply,
        String repliedAt,
        String username,
        String page,
        String device,
        Boolean appInstalled
) {
    static FeedbackDto forPlayer(Feedback f) {
        return new FeedbackDto(f.getId(), f.getCategory(), f.getMessage(), f.getCreatedAt().toString(), f.getStatus(),
                f.getReply(), f.getRepliedAt() == null ? null : f.getRepliedAt().toString(), null, null, null, null);
    }

    static FeedbackDto forAdmin(Feedback f, String username) {
        return new FeedbackDto(f.getId(), f.getCategory(), f.getMessage(), f.getCreatedAt().toString(), f.getStatus(),
                f.getReply(), f.getRepliedAt() == null ? null : f.getRepliedAt().toString(), username,
                f.getPage(), f.getDevice(), f.getAppInstalled());
    }
}
