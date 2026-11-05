package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.BracketPick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BracketPickRepository extends JpaRepository<BracketPick, String> {

    List<BracketPick> findByBracketId(String bracketId);

    long countByBracketId(String bracketId);

    @Modifying
    @Query("DELETE FROM BracketPick p WHERE p.userId = :userId")
    void deleteAllForUser(String userId);
}
