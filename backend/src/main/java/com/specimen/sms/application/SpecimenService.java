package com.specimen.sms.application;

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
import com.specimen.sms.web.dto.Dtos.SpecimenView;
import com.specimen.sms.web.dto.Dtos.StatsView;
import com.specimen.sms.web.dto.Dtos.StatusChangeRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecimenService {

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
        specimens.save(s);
        order.getSpecimens().add(s);

        audit.record("SPECIMEN", s.getId(), "ACCESSIONED",
                "Barcode " + s.getBarcode() + ", tube " + s.getTubeType() + ", status " + s.getStatus());
        audit.record("ORDER", order.getId(), "SPECIMEN_ACCESSIONED", "Barcode " + s.getBarcode());
        return Views.specimen(s, CurrentUser.isAdmin());
    }

    @Transactional
    public SpecimenView changeStatus(Long id, StatusChangeRequest request) {
        Specimen s = load(id);
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
        audit.record("SPECIMEN", s.getId(), "STATUS_CHANGED",
                from + " -> " + to + (override ? " (admin override)" : "") + (reason != null ? ": " + reason : ""));
        return Views.specimen(s, admin);
    }

    /** Adds results from a person (UI/API); attributed to the current user. */
    @Transactional
    public SpecimenView addResults(Long id, List<ResultInput> inputs) {
        return addResults(id, inputs, CurrentUser.username());
    }

    /** Adds results on behalf of {@code actor} (a username or an instrument id) and moves the specimen to RESULTED. */
    @Transactional
    public SpecimenView addResults(Long id, List<ResultInput> inputs, String actor) {
        Specimen s = load(id);
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
    public List<SpecimenView> search(String barcode, String patient, SpecimenStatus status, Long orderId) {
        boolean admin = CurrentUser.isAdmin();
        return specimens.findAll((root, query, cb) -> {
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
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (orderId != null) {
                ps.add(cb.equal(order.get("id"), orderId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        }, PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getContent().stream().map(s -> Views.specimen(s, admin)).toList();
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
}
