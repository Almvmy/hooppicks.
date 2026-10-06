package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.User;

import java.time.Instant;

public record AdminUserDto(
        String id,
        String username,
        String email,
        boolean isAdmin,
        boolean emailVerified,
        int walletBalance,
        Instant createdAt,
        long totalBets,
        String favoriteTeam
) {
    public static AdminUserDto from(User user) {
        return from(user, 0);
    }

    public static AdminUserDto from(User user, long totalBets) {
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isAdmin(),
                user.isEmailVerified(),
                user.getWalletBalance(),
                user.getCreatedAt(),
                totalBets,
                user.getFavoriteTeam() == null || user.getFavoriteTeam().isBlank() ? null : user.getFavoriteTeam()
        );
    }
}
