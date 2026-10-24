package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.JobRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRunRepository extends JpaRepository<JobRun, String> {
}
