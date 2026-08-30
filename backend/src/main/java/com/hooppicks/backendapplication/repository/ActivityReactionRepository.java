package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.ActivityReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityReactionRepository extends JpaRepository<ActivityReaction, String> {

    // targetId seul suffit à filtrer (les ids générés en UUID ne se recoupent
    // pas entre BET et MEMBERSHIP), mais on garde targetType dans la clause
    // pour rester explicite et robuste si un jour les ids ne sont plus des UUID.
    List<ActivityReaction> findByTargetTypeInAndTargetIdIn(List<String> targetTypes, List<String> targetIds);

    Optional<ActivityReaction> findByTargetTypeAndTargetIdAndUser_IdAndEmoji(
            String targetType, String targetId, String userId, String emoji);
}
