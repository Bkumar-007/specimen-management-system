package com.specimen.sms.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderTest> tests = new ArrayList<>();

    @OneToMany(mappedBy = "labOrder")
    @OrderBy("id ASC")
    private List<Specimen> specimens = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 64)
    private String createdBy;

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<OrderTest> getTests() {
        return tests;
    }

    public List<Specimen> getSpecimens() {
        return specimens;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Order-level status derived from its specimens: ORDERED when nothing has been collected,
     * otherwise the least advanced active specimen, or CANCELLED/REJECTED if none remain active.
     */
    public SpecimenStatus deriveStatus() {
        if (specimens.isEmpty()) {
            return SpecimenStatus.ORDERED;
        }
        return specimens.stream()
                .map(Specimen::getStatus)
                .filter(s -> s != SpecimenStatus.CANCELLED && s != SpecimenStatus.REJECTED)
                .min(java.util.Comparator.naturalOrder())
                .orElseGet(() -> specimens.stream().anyMatch(s -> s.getStatus() == SpecimenStatus.REJECTED)
                        ? SpecimenStatus.REJECTED
                        : SpecimenStatus.CANCELLED);
    }
}
