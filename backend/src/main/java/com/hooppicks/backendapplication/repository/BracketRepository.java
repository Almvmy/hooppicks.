package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.Bracket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BracketRepository extends JpaRepository<Bracket, String> {

    Optional<Bracket> findTopByOrderByLocksAtDesc();
}
