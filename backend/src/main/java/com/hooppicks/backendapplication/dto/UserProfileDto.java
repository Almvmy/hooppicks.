package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.User;

public record UserProfileDto(
        String username,
        String email,
        int winRate,
        int totalBets,
        String favoriteTeam
) {
    public static UserProfileDto from(User user, int winRate, int totalBets) {
        return new UserProfileDto(user.getUsername(), user.getEmail(), winRate, totalBets, user.getFavoriteTeam());
    }
}