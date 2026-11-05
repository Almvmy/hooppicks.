package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, String> {

    List<UserBlock> findByBlockerId(String blockerId);

    @Modifying
    @Query("DELETE FROM UserBlock b WHERE b.blockerId = :userId OR b.blockedId = :userId")
    void deleteAllForUser(String userId);
}
