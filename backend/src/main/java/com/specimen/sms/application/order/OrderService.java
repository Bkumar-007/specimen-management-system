package com.specimen.sms.application.order;

import com.specimen.sms.application.audit.AuditService;
import com.specimen.sms.application.common.BusinessRuleException;
import com.specimen.sms.application.common.CodeGenerator;
import com.specimen.sms.application.common.NotFoundException;
import com.specimen.sms.application.specimen.Views;
import com.specimen.sms.domain.Client;
import com.specimen.sms.domain.ClientRepository;
import com.specimen.sms.domain.LabOrder;
import com.specimen.sms.domain.LabOrderRepository;
import com.specimen.sms.domain.OrderTest;
import com.specimen.sms.domain.Patient;
import com.specimen.sms.domain.PatientRepository;
import com.specimen.sms.domain.Specimen;
import com.specimen.sms.domain.SpecimenStatus;
import com.specimen.sms.domain.TestCatalog;
import com.specimen.sms.domain.TestCatalogRepository;
import com.specimen.sms.security.CurrentUser;
import com.specimen.sms.web.dto.Dtos.AuditView;
import com.specimen.sms.web.dto.Dtos.CreateOrderRequest;
import com.specimen.sms.web.dto.Dtos.OrderDetail;
import com.specimen.sms.web.dto.Dtos.OrderSummary;
import com.specimen.sms.web.dto.Dtos.PatientInput;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final LabOrderRepository orders;
    private final ClientRepository clients;
    private final PatientRepository patients;
    private final TestCatalogRepository tests;
    private final AuditService audit;

    public OrderService(
            LabOrderRepository orders,
            ClientRepository clients,
            PatientRepository patients,
            TestCatalogRepository tests,
            AuditService audit) {
        this.orders = orders;
        this.clients = clients;
        this.patients = patients;
        this.tests = tests;
        this.audit = audit;
    }

    @Transactional
    public OrderDetail create(CreateOrderRequest request) {
        Client client = clients.findById(request.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + request.clientId()));

        LinkedHashSet<Long> testIds = new LinkedHashSet<>(request.testIds());
        List<TestCatalog> selected = tests.findAllById(testIds);
        if (selected.size() != testIds.size()) {
            throw new NotFoundException("One or more tests were not found");
        }
        if (selected.stream().anyMatch(t -> !t.isActive())) {
            throw new BusinessRuleException("One or more selected tests are inactive");
        }

        Patient patient = findOrCreatePatient(request.patient());

        LabOrder order = new LabOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setClient(client);
        order.setPatient(patient);
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(CurrentUser.username());
        selected.forEach(t -> order.getTests().add(new OrderTest(order, t)));
        orders.save(order);

        audit.record("ORDER", order.getId(), "CREATED",
                "Order " + order.getOrderNumber() + " for MRN " + patient.getMrn() + ", tests: "
                        + selected.stream().map(TestCatalog::getCode).sorted().toList());
        return Views.orderDetail(order, CurrentUser.isAdmin());
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> search(String q, SpecimenStatus status) {
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        List<LabOrder> found = orders.findAll((root, query, cb) -> {
            var patient = root.join("patient");
            List<Predicate> ps = new ArrayList<>();
            if (pattern != null) {
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(patient.get("mrn")), pattern),
                        cb.like(cb.lower(patient.get("firstName")), pattern),
                        cb.like(cb.lower(patient.get("lastName")), pattern)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        }, PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).getContent();

        return found.stream()
                .map(Views::orderSummary)
                .filter(s -> status == null || s.status() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetail get(Long id) {
        return Views.orderDetail(load(id), CurrentUser.isAdmin());
    }

    /**
     * Order history: order audit plus each specimen's audit (barcode tagged), chronological.
     * Statuses are the full lab set (ORDERED…REPORTED, CANCELLED/REJECTED).
     */
    @Transactional(readOnly = true)
    public List<AuditView> timeline(Long id) {
        LabOrder order = load(id);
        List<AuditView> events = new ArrayList<>(audit.trail("ORDER", id).stream().map(Views::audit).toList());
        for (Specimen s : order.getSpecimens()) {
            String barcode = s.getBarcode();
            audit.trail("SPECIMEN", s.getId()).stream()
                    .map(a -> new AuditView(
                            a.getId(),
                            a.getEntityType(),
                            a.getEntityId(),
                            a.getAction(),
                            "[" + barcode + "] " + (a.getDetails() == null ? "" : a.getDetails()),
                            a.getPerformedBy(),
                            a.getPerformedAt()))
                    .forEach(events::add);
        }
        events.sort(Comparator
                .comparing(AuditView::performedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AuditView::id, Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    /** Hard-delete only when no specimens exist (still ORDERED). */
    @Transactional
    public void delete(Long id) {
        LabOrder order = load(id);
        if (!order.getSpecimens().isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot delete an order that has specimens. Cancel or reject the specimens instead.");
        }
        String number = order.getOrderNumber();
        audit.record("ORDER", order.getId(), "DELETED", "Order " + number + " removed");
        orders.delete(order);
    }

    private LabOrder load(Long id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    private Patient findOrCreatePatient(PatientInput in) {
        String mrn = in.mrn().trim();
        return patients.findByMrnIgnoreCase(mrn).orElseGet(() -> {
            Patient p = new Patient();
            p.setMrn(mrn);
            p.setFirstName(in.firstName().trim());
            p.setLastName(in.lastName().trim());
            p.setDateOfBirth(in.dateOfBirth());
            p.setSex(in.sex() == null || in.sex().isBlank() ? null : in.sex().trim());
            return patients.save(p);
        });
    }

    private String generateOrderNumber() {
        String day = DateTimeFormatter.ofPattern("yyMMdd").format(LocalDate.now(ZoneOffset.UTC));
        String number;
        do {
            number = "ORD-" + day + "-" + CodeGenerator.digits(5);
        } while (orders.existsByOrderNumber(number));
        return number;
    }
}
