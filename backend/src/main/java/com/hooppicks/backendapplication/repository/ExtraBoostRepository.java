package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.ExtraBoost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExtraBoostRepository extends JpaRepository<ExtraBoost, String> {

    List<ExtraBoost> findByNight(LocalDate night);
}
