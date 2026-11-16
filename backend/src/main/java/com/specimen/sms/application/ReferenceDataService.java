package com.specimen.sms.application;

import com.specimen.sms.domain.Client;
import com.specimen.sms.domain.ClientRepository;
import com.specimen.sms.domain.TestCatalog;
import com.specimen.sms.domain.TestCatalogRepository;
import com.specimen.sms.web.dto.Dtos.ClientRequest;
import com.specimen.sms.web.dto.Dtos.ClientView;
import com.specimen.sms.web.dto.Dtos.TestRequest;
import com.specimen.sms.web.dto.Dtos.TestView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clients (sites) and the test catalog. */
@Service
public class ReferenceDataService {

    private final ClientRepository clients;
    private final TestCatalogRepository tests;
    private final AuditService audit;

    public ReferenceDataService(ClientRepository clients, TestCatalogRepository tests, AuditService audit) {
        this.clients = clients;
        this.tests = tests;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ClientView> listClients() {
        return clients.findAllByOrderByNameAsc().stream().map(Views::client).toList();
    }

    @Transactional
    public ClientView createClient(ClientRequest request) {
        String code = request.code().trim().toUpperCase();
        if (clients.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Client code already exists: " + code);
        }
        Client c = new Client();
        c.setCode(code);
        c.setName(request.name().trim());
        clients.save(c);
        audit.record("CLIENT", c.getId(), "CREATED", c.getCode() + " - " + c.getName());
        return Views.client(c);
    }

    @Transactional(readOnly = true)
    public List<TestView> listTests() {
        return tests.findAllByOrderByCodeAsc().stream().filter(TestCatalog::isActive).map(Views::test).toList();
    }

    @Transactional
    public TestView createTest(TestRequest request) {
        String code = request.code().trim().toUpperCase();
        if (tests.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Test code already exists: " + code);
        }
        TestCatalog t = new TestCatalog();
        t.setCode(code);
        t.setName(request.name().trim());
        t.setTubeType(request.tubeType().trim());
        tests.save(t);
        audit.record("TEST", t.getId(), "CREATED", t.getCode() + " - " + t.getName());
        return Views.test(t);
    }
}
