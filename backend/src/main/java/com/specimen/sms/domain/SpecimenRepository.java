package com.specimen.sms.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SpecimenRepository extends JpaRepository<Specimen, Long>, JpaSpecificationExecutor<Specimen> {
    Optional<Specimen> findByBarcodeIgnoreCase(String barcode);

    boolean existsByBarcode(String barcode);

    long countByStatus(SpecimenStatus status);
}
