package com.specimen.sms.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A test ordered on a lab order. */
@Entity
@Table(name = "order_tests")
public class OrderTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id")
    private TestCatalog test;

    protected OrderTest() {
    }

    public OrderTest(LabOrder labOrder, TestCatalog test) {
        this.labOrder = labOrder;
        this.test = test;
    }

    public Long getId() {
        return id;
    }

    public LabOrder getLabOrder() {
        return labOrder;
    }

    public TestCatalog getTest() {
        return test;
    }
}
