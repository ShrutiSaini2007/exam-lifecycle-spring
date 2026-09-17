package com.examlifecycle.controller;

import com.examlifecycle.domain.Role;
import com.examlifecycle.dto.DraftDtos.AuditLogView;
import com.examlifecycle.security.AuthUtil;
import com.examlifecycle.service.DraftService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final DraftService draftService;

    public AuditController(DraftService draftService) {
        this.draftService = draftService;
    }

    @GetMapping
    public List<AuditLogView> globalAudit() {
        var user = AuthUtil.current();
        AuthUtil.requireRole(user, Role.ADMIN, Role.EXAM_OFFICER);
        return draftService.globalAudit();
    }
}
