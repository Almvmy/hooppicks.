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
        Instant createdAt
) {
    public static AdminUserDto from(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isAdmin(),
                user.isEmailVerified(),
                user.getWalletBalance(),
                user.getCreatedAt()
        );
    }
}
