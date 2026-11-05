package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.MessageReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface MessageReportRepository extends JpaRepository<MessageReport, String> {

    long countByMessageId(String messageId);

    List<MessageReport> findByMessageIdIn(Collection<String> messageIds);

    @Modifying
    @Query("DELETE FROM MessageReport r WHERE r.messageId = :messageId")
    void deleteAllForMessage(String messageId);

    @Query("SELECT DISTINCT r.messageId FROM MessageReport r")
    List<String> findReportedMessageIds();
}
