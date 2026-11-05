package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Debate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DebateRepository extends JpaRepository<Debate, LocalDate> {

    List<Debate> findTop7ByDayLessThanOrderByDayDesc(LocalDate day);
}
