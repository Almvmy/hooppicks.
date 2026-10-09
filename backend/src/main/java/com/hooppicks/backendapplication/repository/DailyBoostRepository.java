package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.DailyBoost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyBoostRepository extends JpaRepository<DailyBoost, LocalDate> {
}
