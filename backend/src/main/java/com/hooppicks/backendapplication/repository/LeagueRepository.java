package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, String> {
    Optional<League> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    List<League> findByOwnerId(String ownerId);
}
