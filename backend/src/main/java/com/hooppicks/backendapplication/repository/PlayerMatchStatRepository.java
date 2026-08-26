package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.PlayerMatchStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerMatchStatRepository extends JpaRepository<PlayerMatchStat, String> {
    List<PlayerMatchStat> findByMatchIdOrderByPointsDesc(String matchId);
}
