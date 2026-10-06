package com.hooppicks.backendapplication.admin;

import com.hooppicks.backendapplication.entity.AdminAuditLog;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.AdminAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);
    private static final int MAX_DETAILS = 500;

    private final AdminAuditLogRepository repository;

    public AdminAuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(User admin, String action, String target, String details) {
        AdminAuditLog entry = new AdminAuditLog();
        entry.setAdminId(admin.getId());
        entry.setAdminUsername(admin.getUsername());
        entry.setAction(action);
        entry.setTarget(target);
        entry.setDetails(details == null || details.length() <= MAX_DETAILS ? details : details.substring(0, MAX_DETAILS));
        repository.save(entry);
        // Aussi dans les logs serveur : ils restent lisibles même si la base
        // est en cause (c'est souvent dans ce cas qu'on les consulte).
        log.info("Console admin : {} par {} sur {} ({})", action, admin.getUsername(), target, details);
    }

    public List<AdminAuditLog> recent(int limit) {
        return repository.findAllByOrderByDateDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }
}
