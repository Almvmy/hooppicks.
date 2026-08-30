package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.User;

public record UserProfileDto(
        String username,
        String email,
        int winRate,
        int totalBets,
        int currentWinStreak,
        String favoriteTeam,
        int avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon,
        boolean isAdmin,
        boolean notifyMatchStarting,
        boolean notifyBetResults,
        boolean notifyLeagueActivity,
        boolean emailVerified
) {
    public static UserProfileDto from(User user, int winRate, int totalBets, int currentWinStreak) {
        return new UserProfileDto(
                user.getUsername(),
                user.getEmail(),
                winRate,
                totalBets,
                currentWinStreak,
                user.getFavoriteTeam(),
                user.getAvatarNumber(),
                user.getAvatarPosition(),
                user.getAvatarColorway(),
                user.getAvatarIcon(),
                user.isAdmin(),
                user.isNotifyMatchStarting(),
                user.isNotifyBetResults(),
                user.isNotifyLeagueActivity(),
                user.isEmailVerified()
        );
    }
}
