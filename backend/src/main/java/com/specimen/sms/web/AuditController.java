package com.specimen.sms.web;

import com.specimen.sms.application.audit.AuditService;
import com.specimen.sms.application.specimen.Views;
import com.specimen.sms.web.dto.Dtos.AuditView;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('LAB_ADMIN')")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public List<AuditView> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        return audit.search(entityType, entityId).stream().map(Views::audit).toList();
    }
}
