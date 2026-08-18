package com.hooppicks.backendapplication.dto;

/**
 * Tous les champs sont optionnels : seuls ceux fournis (non null) sont
 * appliqués, ce qui permet de mettre à jour l'équipe favorite et l'avatar
 * indépendamment depuis le même endpoint.
 */
public record UpdateProfileRequest(
        String favoriteTeam,
        Integer avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon
) {
}
