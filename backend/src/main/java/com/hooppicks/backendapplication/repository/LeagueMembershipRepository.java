package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.LeagueMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueMembershipRepository extends JpaRepository<LeagueMembership, String> {
    List<LeagueMembership> findByUserId(String userId);

    List<LeagueMembership> findByLeagueId(String leagueId);

    Optional<LeagueMembership> findByLeagueIdAndUserId(String leagueId, String userId);

    long countByLeagueId(String leagueId);
}
