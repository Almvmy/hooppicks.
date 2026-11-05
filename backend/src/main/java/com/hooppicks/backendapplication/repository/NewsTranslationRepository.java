package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.NewsTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface NewsTranslationRepository extends JpaRepository<NewsTranslation, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM NewsTranslation t WHERE t.createdAt < :before")
    int deleteOlderThan(Instant before);
}
