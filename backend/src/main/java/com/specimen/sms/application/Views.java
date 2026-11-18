package com.specimen.sms.application;

import com.specimen.sms.domain.AuditLog;
import com.specimen.sms.domain.Client;
import com.specimen.sms.domain.LabOrder;
import com.specimen.sms.domain.OrderTest;
import com.specimen.sms.domain.Patient;
import com.specimen.sms.domain.Specimen;
import com.specimen.sms.domain.SpecimenResult;
import com.specimen.sms.domain.TestCatalog;
import com.specimen.sms.web.dto.Dtos.AuditView;
import com.specimen.sms.web.dto.Dtos.ClientView;
import com.specimen.sms.web.dto.Dtos.OrderDetail;
import com.specimen.sms.web.dto.Dtos.OrderSummary;
import com.specimen.sms.web.dto.Dtos.PatientView;
import com.specimen.sms.web.dto.Dtos.ResultView;
import com.specimen.sms.web.dto.Dtos.SpecimenView;
import com.specimen.sms.web.dto.Dtos.TestView;
import java.util.Comparator;
import java.util.List;

/** Entity to DTO mapping. Call inside a transaction (touches lazy associations). */
public final class Views {

    private Views() {
    }

    public static ClientView client(Client c) {
        return new ClientView(c.getId(), c.getCode(), c.getName());
    }

    public static TestView test(TestCatalog t) {
        return new TestView(t.getId(), t.getCode(), t.getName(), t.getTubeType(), t.isActive());
    }

    public static PatientView patient(Patient p) {
        return new PatientView(p.getId(), p.getMrn(), p.getFirstName(), p.getLastName(), p.getDateOfBirth(), p.getSex());
    }

    public static ResultView result(SpecimenResult r) {
        return new ResultView(r.getId(), r.getAnalyte(), r.getValue(), r.getUnit(), r.isFlagged(), r.getResultedAt(),
                r.getEnteredBy());
    }

    public static SpecimenView specimen(Specimen s, boolean admin) {
        LabOrder o = s.getLabOrder();
        return new SpecimenView(
                s.getId(),
                s.getBarcode(),
                o.getId(),
                o.getOrderNumber(),
                o.getPatient().getFullName(),
                o.getPatient().getMrn(),
                s.getTubeType(),
                s.getCollectedAt(),
                s.getReceivedAt(),
                s.getStatus(),
                s.getStatusReason(),
                List.copyOf(StatusTransitionPolicy.allowedNext(s.getStatus(), admin)),
                s.getResults().stream().map(Views::result).toList(),
                s.getVersion(),
                s.getCreatedAt());
    }

    public static OrderSummary orderSummary(LabOrder o) {
        return new OrderSummary(
                o.getId(),
                o.getOrderNumber(),
                o.deriveStatus(),
                o.getClient().getName(),
                o.getPatient().getFullName(),
                o.getPatient().getMrn(),
                o.getTests().stream().map(t -> t.getTest().getCode()).sorted().toList(),
                o.getSpecimens().size(),
                o.getCreatedAt());
    }

    public static OrderDetail orderDetail(LabOrder o, boolean admin) {
        List<TestView> tests = o.getTests().stream()
                .map(OrderTest::getTest)
                .sorted(Comparator.comparing(TestCatalog::getCode))
                .map(Views::test)
                .toList();
        return new OrderDetail(
                o.getId(),
                o.getOrderNumber(),
                o.deriveStatus(),
                client(o.getClient()),
                patient(o.getPatient()),
                tests,
                o.getSpecimens().stream().map(s -> specimen(s, admin)).toList(),
                o.getCreatedAt(),
                o.getCreatedBy());
    }

    public static AuditView audit(AuditLog a) {
        return new AuditView(a.getId(), a.getEntityType(), a.getEntityId(), a.getAction(), a.getDetails(),
                a.getPerformedBy(), a.getPerformedAt());
    }
}
