package com.specimen.sms.application;

import com.specimen.sms.domain.AuditLog;
import com.specimen.sms.domain.AuditLogRepository;
import com.specimen.sms.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Records an audit entry in the caller's transaction, attributed to the current user. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String entityType, Long entityId, String action, String details) {
        record(entityType, entityId, action, details, CurrentUser.username());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String entityType, Long entityId, String action, String details, String actor) {
        if (details != null && details.length() > 1000) {
            details = details.substring(0, 1000);
        }
        repository.save(new AuditLog(entityType, entityId, action, details, actor));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> trail(String entityType, Long entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByPerformedAtAscIdAsc(entityType.toUpperCase(), entityId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(String entityType, Long entityId) {
        return repository.findAll((root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (entityType != null && !entityType.isBlank()) {
                ps.add(cb.equal(root.get("entityType"), entityType.trim().toUpperCase()));
            }
            if (entityId != null) {
                ps.add(cb.equal(root.get("entityId"), entityId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        }, PageRequest.of(0, 300, Sort.by(Sort.Direction.DESC, "performedAt", "id"))).getContent();
    }
}
