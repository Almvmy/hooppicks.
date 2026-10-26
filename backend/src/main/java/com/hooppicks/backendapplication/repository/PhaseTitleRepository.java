package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.PhaseTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PhaseTitleRepository extends JpaRepository<PhaseTitle, String> {

    List<PhaseTitle> findByUserIdOrderBySeasonDesc(String userId);

    boolean existsByUserId(String userId);

    boolean existsByUserIdAndPhaseAndLeagueIdIsNull(String userId, String phase);

    List<PhaseTitle> findBySeasonAndPhaseAndLeagueIdIsNull(String season, String phase);

    List<PhaseTitle> findBySeasonAndPhaseAndLeagueId(String season, String phase, String leagueId);

    @Modifying
    @Query("DELETE FROM PhaseTitle t WHERE t.userId = :userId")
    void deleteAllForUser(String userId);
}
