package com.examlifecycle.service;

import com.examlifecycle.domain.AuditLog;
import com.examlifecycle.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper mapper) {
        this.auditLogRepository = auditLogRepository;
        this.mapper = mapper;
    }

    public void record(Long userId, String action, String entityType, Long entityId, Map<String, Object> details) {
        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = mapper.writeValueAsString(details);
            } catch (Exception ignored) {
                // Never let audit-log serialization failure break the actual operation being audited.
            }
        }
        auditLogRepository.save(new AuditLog(userId, action, entityType, entityId, detailsJson, currentIp()));
    }

    public void record(Long userId, String action, String entityType, Long entityId) {
        record(userId, action, entityType, entityId, null);
    }

    private String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
