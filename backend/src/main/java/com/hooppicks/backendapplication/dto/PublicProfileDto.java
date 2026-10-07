package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.User;

import java.util.List;

/**
 * Sous-ensemble "sûr à montrer à n'importe qui" de User : jamais l'email,
 * jamais isAdmin/notifyXxx. Contrairement à UserProfileDto (réservé au
 * propriétaire du compte via /auth/me).
 *
 * Tickets : seulement ceux déjà réglés (gagnés, perdus, remboursés). Les
 * tickets en attente restent privés : sinon on pourrait copier les paris en
 * cours d'un bon joueur avant le coup d'envoi.
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
        List<BadgeDto> badges,
        String memberSince,
        long seasonPoints,
        Integer seasonRank,
        int seasonPlayers,
        long weekPoints,
        Integer weekRank,
        int weekPlayers,
        int currentStreak,
        int bestStreak,
        List<PlacedBetDto> recentTickets,
        PlacedBetDto bestTicket,
        List<String> commonLeagues,
        boolean isMe
) {
    public record Extras(String memberSince, long seasonPoints, Integer seasonRank, int seasonPlayers,
                         long weekPoints, Integer weekRank, int weekPlayers, int currentStreak, int bestStreak,
                         List<PlacedBetDto> recentTickets, PlacedBetDto bestTicket,
                         List<String> commonLeagues, boolean isMe) {}

    public static PublicProfileDto from(User user, int winRate, int totalBets, List<BadgeDto> badges, Extras x) {
        return new PublicProfileDto(
                user.getUsername(),
                winRate,
                totalBets,
                user.getFavoriteTeam(),
                user.getAvatarNumber(),
                user.getAvatarPosition(),
                user.getAvatarColorway(),
                user.getAvatarIcon(),
                badges,
                x.memberSince(), x.seasonPoints(), x.seasonRank(), x.seasonPlayers(),
                x.weekPoints(), x.weekRank(), x.weekPlayers(), x.currentStreak(), x.bestStreak(),
                x.recentTickets(), x.bestTicket(), x.commonLeagues(), x.isMe()
        );
    }
}
