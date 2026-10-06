package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.ActivityReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityReactionRepository extends JpaRepository<ActivityReaction, String> {

    // targetId seul suffit à filtrer (les ids générés en UUID ne se recoupent
    // pas entre BET et MEMBERSHIP), mais on garde targetType dans la clause
    // pour rester explicite et robuste si un jour les ids ne sont plus des UUID.
    List<ActivityReaction> findByTargetTypeInAndTargetIdIn(List<String> targetTypes, List<String> targetIds);

    // Toutes les réactions d'un membre sur un item : une seule en principe
    // (cf. LeagueService.toggleReaction), plusieurs pour les données
    // antérieures à cette règle, nettoyées au prochain clic.
    List<ActivityReaction> findByTargetTypeAndTargetIdAndUser_Id(String targetType, String targetId, String userId);
}
