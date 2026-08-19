package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.Player;

public record PlayerDto(
        String id,
        String firstName,
        String lastName,
        String position,
        String height,
        String weight,
        TeamDto team
) {
    public static PlayerDto from(Player player) {
        return new PlayerDto(
                player.getId(),
                player.getFirstName(),
                player.getLastName(),
                player.getPosition(),
                player.getHeight(),
                player.getWeight(),
                player.getTeam() != null ? TeamDto.from(player.getTeam()) : null
        );
    }
}
