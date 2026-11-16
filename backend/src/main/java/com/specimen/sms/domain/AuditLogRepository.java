package com.specimen.sms.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtAscIdAsc(String entityType, Long entityId);
}
