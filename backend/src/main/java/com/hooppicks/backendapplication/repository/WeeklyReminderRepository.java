package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.WeeklyReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReminderRepository extends JpaRepository<WeeklyReminder, String> {
}
