package com.hooppicks.backendapplication.repository;


import com.hooppicks.backendapplication.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, String> {
}