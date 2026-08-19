package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, String> {
    Optional<Player> findByExternalId(Long externalId);
    List<Player> findByTeamIdOrderByLastNameAsc(String teamId);
    List<Player> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
}
