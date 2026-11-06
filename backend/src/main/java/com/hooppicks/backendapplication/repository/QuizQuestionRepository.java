package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, String> {

    List<QuizQuestion> findByDayOrderByPositionAsc(LocalDate day);

    List<QuizQuestion> findByDayGreaterThanEqual(LocalDate day);
}
