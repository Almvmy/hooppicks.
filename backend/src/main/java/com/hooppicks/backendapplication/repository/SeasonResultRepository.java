package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.SeasonResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonResultRepository extends JpaRepository<SeasonResult, String> {

    List<SeasonResult> findBySeason(String season);

    Optional<SeasonResult> findBySeasonAndQuestion(String season, String question);
}
