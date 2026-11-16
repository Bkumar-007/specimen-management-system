package com.specimen.sms.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCatalogRepository extends JpaRepository<TestCatalog, Long> {
    List<TestCatalog> findAllByOrderByCodeAsc();

    boolean existsByCodeIgnoreCase(String code);
}
