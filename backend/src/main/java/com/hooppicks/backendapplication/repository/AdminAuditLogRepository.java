package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.AdminAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, String> {
    List<AdminAuditLog> findAllByOrderByDateDesc(Pageable pageable);
}
