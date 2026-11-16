package com.specimen.sms.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByMrnIgnoreCase(String mrn);
}
