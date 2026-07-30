package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface MatchRepository extends JpaRepository<Match, String> {
    Optional<Match> findByExternalId(Long externalId);
}