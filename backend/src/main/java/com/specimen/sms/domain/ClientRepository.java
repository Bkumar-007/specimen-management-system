package com.specimen.sms.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findAllByOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
