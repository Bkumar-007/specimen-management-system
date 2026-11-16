package com.specimen.sms.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "specimens")
public class Specimen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private LabOrder labOrder;

    @Column(nullable = false, unique = true, length = 32)
    private String barcode;

    @Column(nullable = false, length = 64)
    private String tubeType;

    private Instant collectedAt;

    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SpecimenStatus status;

    @Column(length = 500)
    private String statusReason;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "specimen", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SpecimenResult> results = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public LabOrder getLabOrder() {
        return labOrder;
    }

    public void setLabOrder(LabOrder labOrder) {
        this.labOrder = labOrder;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getTubeType() {
        return tubeType;
    }

    public void setTubeType(String tubeType) {
        this.tubeType = tubeType;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public SpecimenStatus getStatus() {
        return status;
    }

    public void setStatus(SpecimenStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<SpecimenResult> getResults() {
        return results;
    }

    public void addResult(SpecimenResult result) {
        result.setSpecimen(this);
        results.add(result);
    }
}
