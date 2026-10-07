package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.PalmaresWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface PalmaresWeekRepository extends JpaRepository<PalmaresWeek, LocalDate> {
}
