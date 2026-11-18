package com.specimen.sms.application.specimen;

import com.specimen.sms.application.audit.AuditService;
import com.specimen.sms.application.common.BusinessRuleException;
import com.specimen.sms.application.common.CodeGenerator;
import com.specimen.sms.application.common.NotFoundException;
import com.specimen.sms.domain.LabOrder;
import com.specimen.sms.domain.LabOrderRepository;
import com.specimen.sms.domain.Specimen;
import com.specimen.sms.domain.SpecimenRepository;
import com.specimen.sms.domain.SpecimenResult;
import com.specimen.sms.domain.SpecimenStatus;
import com.specimen.sms.security.CurrentUser;
import com.specimen.sms.web.dto.Dtos.AccessionRequest;
import com.specimen.sms.web.dto.Dtos.AuditView;
import com.specimen.sms.web.dto.Dtos.ResultInput;
import com.specimen.sms.web.dto.Dtos.SpecimenPage;
import com.specimen.sms.web.dto.Dtos.SpecimenView;
import com.specimen.sms.web.dto.Dtos.StatsView;
import com.specimen.sms.web.dto.Dtos.StatusChangeRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecimenService {

    /** Statuses that still need bench action (work queue). */
    public static final Set<SpecimenStatus> ACTIONABLE = EnumSet.of(
            SpecimenStatus.COLLECTED,
            SpecimenStatus.RECEIVED,
            SpecimenStatus.IN_TESTING,
            SpecimenStatus.RESULTED);

    private static final int MAX_PAGE_SIZE = 100;

    private final SpecimenRepository specimens;
    private final LabOrderRepository orders;
    private final AuditService audit;

    public SpecimenService(SpecimenRepository specimens, LabOrderRepository orders, AuditService audit) {
        this.specimens = specimens;
        this.orders = orders;
        this.audit = audit;
    }

    @Transactional
    public SpecimenView accession(Long orderId, AccessionRequest request) {
        LabOrder order = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        Instant now = Instant.now();
        Instant collectedAt = request.collectedAt() != null ? request.collectedAt() : now;
        if (collectedAt.isAfter(now.plusSeconds(60))) {
            throw new BusinessRuleException("Collection time cannot be in the future");
        }
        boolean receive = request.markReceived() == null || request.markReceived();

        Specimen s = new Specimen();
        s.setLabOrder(order);
        s.setBarcode(generateBarcode());
        s.setTubeType(request.tubeType().trim());
        s.setCollectedAt(collectedAt);
        s.setCreatedAt(now);
        if (receive) {
            s.setReceivedAt(now);
            s.setStatus(SpecimenStatus.RECEIVED);
        } else {
            s.setStatus(SpecimenStatus.COLLECTED);
        }
        specimens.saveAndFlush(s);
        order.getSpecimens().add(s);

        audit.record("SPECIMEN", s.getId(), "ACCESSIONED",
                "Barcode " + s.getBarcode() + ", tube " + s.getTubeType() + ", status " + s.getStatus());
        audit.record("ORDER", order.getId(), "SPECIMEN_ACCESSIONED", "Barcode " + s.getBarcode());
        return Views.specimen(s, CurrentUser.isAdmin());
    }

    @Transactional
    public SpecimenView changeStatus(Long id, StatusChangeRequest request) {
        Specimen s = load(id);
        assertVersion(s, request.version());
        boolean admin = CurrentUser.isAdmin();
        boolean override = Boolean.TRUE.equals(request.override());
        SpecimenStatus from = s.getStatus();
        SpecimenStatus to = request.status();

        StatusTransitionPolicy.validate(from, to, !s.getResults().isEmpty(), admin, override, request.reason());

        s.setStatus(to);
        if (to == SpecimenStatus.RECEIVED) {
            s.setReceivedAt(Instant.now());
        }
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        if (reason != null) {
            s.setStatusReason(reason);
        }
        specimens.saveAndFlush(s); // flush so @Version is incremented before the response is built
        audit.record("SPECIMEN", s.getId(), "STATUS_CHANGED",
                from + " -> " + to + (override ? " (admin override)" : "") + (reason != null ? ": " + reason : ""));
        return Views.specimen(s, admin);
    }

    /** Adds results from a person (UI/API); attributed to the current user. */
    @Transactional
    public SpecimenView addResults(Long id, Long version, List<ResultInput> inputs) {
        return addResults(id, version, inputs, CurrentUser.username());
    }

    /**
     * Adds results on behalf of {@code actor} (instrument ingest). Uses the current DB version so
     * analyzers do not need a client-supplied token; JPA {@code @Version} still guards concurrent writes.
     */
    @Transactional
    public SpecimenView addResults(Long id, List<ResultInput> inputs, String actor) {
        Specimen s = load(id);
        return addResults(id, s.getVersion(), inputs, actor);
    }

    /** Adds results on behalf of {@code actor} and moves the specimen to RESULTED. */
    @Transactional
    public SpecimenView addResults(Long id, Long version, List<ResultInput> inputs, String actor) {
        Specimen s = load(id);
        assertVersion(s, version);
        StatusTransitionPolicy.validateResultEntry(s.getStatus());
        SpecimenStatus from = s.getStatus();

        Instant now = Instant.now();
        for (ResultInput in : inputs) {
            SpecimenResult r = new SpecimenResult();
            r.setAnalyte(in.analyte().trim());
            r.setValue(in.value().trim());
            r.setUnit(in.unit() == null || in.unit().isBlank() ? null : in.unit().trim());
            r.setFlagged(Boolean.TRUE.equals(in.flagged()));
            r.setResultedAt(now);
            r.setEnteredBy(actor.length() > 64 ? actor.substring(0, 64) : actor);
            s.addResult(r);
        }
        s.setStatus(SpecimenStatus.RESULTED);
        specimens.saveAndFlush(s); // flush so @Version is incremented before the response is built

        String analytes = inputs.stream().map(i -> i.analyte().trim()).toList().toString();
        audit.record("SPECIMEN", s.getId(), "RESULTS_ADDED", inputs.size() + " result(s): " + analytes, actor);
        audit.record("SPECIMEN", s.getId(), "STATUS_CHANGED", from + " -> " + SpecimenStatus.RESULTED, actor);
        return Views.specimen(s, CurrentUser.isAdmin());
    }

    @Transactional(readOnly = true)
    public SpecimenView get(Long id) {
        return Views.specimen(load(id), CurrentUser.isAdmin());
    }

    @Transactional(readOnly = true)
    public Long findIdByBarcode(String barcode) {
        return specimens.findByBarcodeIgnoreCase(barcode.trim())
                .map(Specimen::getId)
                .orElseThrow(() -> new NotFoundException("No specimen with barcode " + barcode));
    }

    @Transactional(readOnly = true)
    public SpecimenPage search(
            String barcode,
            String patient,
            SpecimenStatus status,
            List<SpecimenStatus> statuses,
            Long orderId,
            LocalDate from,
            LocalDate to,
            boolean needsAction,
            int page,
            int size) {
        boolean admin = CurrentUser.isAdmin();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        Set<SpecimenStatus> statusFilter = resolveStatuses(status, statuses, needsAction);

        Page<Specimen> result = specimens.findAll((root, query, cb) -> {
            var order = root.join("labOrder");
            var p = order.join("patient");
            List<Predicate> ps = new ArrayList<>();
            if (barcode != null && !barcode.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("barcode")), "%" + barcode.trim().toLowerCase() + "%"));
            }
            if (patient != null && !patient.isBlank()) {
                String like = "%" + patient.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(p.get("mrn")), like),
                        cb.like(cb.lower(p.get("firstName")), like),
                        cb.like(cb.lower(p.get("lastName")), like)));
            }
            if (!statusFilter.isEmpty()) {
                ps.add(root.get("status").in(statusFilter));
            }
            if (orderId != null) {
                ps.add(cb.equal(order.get("id"), orderId));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (to != null) {
                ps.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        }, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

        List<SpecimenView> content = result.getContent().stream().map(s -> Views.specimen(s, admin)).toList();
        return new SpecimenPage(content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    private static Set<SpecimenStatus> resolveStatuses(
            SpecimenStatus status, List<SpecimenStatus> statuses, boolean needsAction) {
        Set<SpecimenStatus> filter = EnumSet.noneOf(SpecimenStatus.class);
        if (status != null) {
            filter.add(status);
        }
        if (statuses != null) {
            for (SpecimenStatus s : statuses) {
                if (s != null) {
                    filter.add(s);
                }
            }
        }
        if (needsAction) {
            if (filter.isEmpty()) {
                return EnumSet.copyOf(ACTIONABLE);
            }
            filter.retainAll(ACTIONABLE);
        }
        return filter;
    }

    @Transactional(readOnly = true)
    public List<AuditView> timeline(Long id) {
        load(id);
        return audit.trail("SPECIMEN", id).stream().map(Views::audit).toList();
    }

    @Transactional(readOnly = true)
    public StatsView stats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (SpecimenStatus st : SpecimenStatus.values()) {
            if (st != SpecimenStatus.ORDERED) {
                byStatus.put(st.name(), specimens.countByStatus(st));
            }
        }
        return new StatsView(byStatus, orders.count(), specimens.count());
    }

    private Specimen load(Long id) {
        return specimens.findById(id).orElseThrow(() -> new NotFoundException("Specimen not found: " + id));
    }

    private String generateBarcode() {
        String day = DateTimeFormatter.ofPattern("yyMMdd").format(LocalDate.now(ZoneOffset.UTC));
        String code;
        do {
            code = "SPC" + day + CodeGenerator.digits(5);
        } while (specimens.existsByBarcode(code));
        return code;
    }

    private void assertVersion(Specimen s, Long expected) {
        if (expected == null || !expected.equals(s.getVersion())) {
            throw new BusinessRuleException(
                    "Specimen was updated by someone else. Refresh and try again.");
        }
    }
}
