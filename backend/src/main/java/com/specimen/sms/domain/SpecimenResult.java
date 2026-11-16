package com.specimen.sms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "specimen_results")
public class SpecimenResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specimen_id")
    private Specimen specimen;

    @Column(nullable = false, length = 64)
    private String analyte;

    @Column(name = "result_value", nullable = false, length = 128)
    private String value;

    @Column(length = 32)
    private String unit;

    @Column(nullable = false)
    private boolean flagged;

    @Column(nullable = false)
    private Instant resultedAt;

    /** Username of the person, or "instrument:ID" for simulated analyzer posts. */
    @Column(nullable = false, length = 64)
    private String enteredBy;

    public Long getId() {
        return id;
    }

    public Specimen getSpecimen() {
        return specimen;
    }

    public void setSpecimen(Specimen specimen) {
        this.specimen = specimen;
    }

    public String getAnalyte() {
        return analyte;
    }

    public void setAnalyte(String analyte) {
        this.analyte = analyte;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public Instant getResultedAt() {
        return resultedAt;
    }

    public void setResultedAt(Instant resultedAt) {
        this.resultedAt = resultedAt;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(String enteredBy) {
        this.enteredBy = enteredBy;
    }
}
