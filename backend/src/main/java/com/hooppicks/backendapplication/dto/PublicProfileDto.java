package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.User;

import java.util.List;

/**
 * Sous-ensemble "sûr à montrer à n'importe qui" de User : jamais l'email,
 * jamais isAdmin/notifyXxx. Contrairement à UserProfileDto (réservé au
 * propriétaire du compte via /auth/me).
 */
public record PublicProfileDto(
        String username,
        int winRate,
        int totalBets,
        String favoriteTeam,
        int avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon,
        List<BadgeDto> badges
) {
    public static PublicProfileDto from(User user, int winRate, int totalBets, List<BadgeDto> badges) {
        return new PublicProfileDto(
                user.getUsername(),
                winRate,
                totalBets,
                user.getFavoriteTeam(),
                user.getAvatarNumber(),
                user.getAvatarPosition(),
                user.getAvatarColorway(),
                user.getAvatarIcon(),
                badges
        );
    }
}
