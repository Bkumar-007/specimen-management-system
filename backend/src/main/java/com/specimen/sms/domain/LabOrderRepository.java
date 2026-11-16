package com.specimen.sms.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long>, JpaSpecificationExecutor<LabOrder> {
    boolean existsByOrderNumber(String orderNumber);
}
