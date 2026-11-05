package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.LeagueMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface LeagueMessageRepository extends JpaRepository<LeagueMessage, String> {

    List<LeagueMessage> findTop30ByLeagueIdOrderByCreatedAtDesc(String leagueId);

    long countByUser_IdAndCreatedAtAfter(String userId, Instant since);

    @Modifying
    @Query("DELETE FROM LeagueMessage m WHERE m.leagueId = :leagueId")
    void deleteAllForLeague(String leagueId);

    @Modifying
    @Query("DELETE FROM LeagueMessage m WHERE m.user.id = :userId")
    void deleteAllForUser(String userId);
}
