package com.specimen.sms.web;

import com.specimen.sms.application.referencedata.ReferenceDataService;
import com.specimen.sms.web.dto.Dtos.ClientRequest;
import com.specimen.sms.web.dto.Dtos.ClientView;
import com.specimen.sms.web.dto.Dtos.TestRequest;
import com.specimen.sms.web.dto.Dtos.TestView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('LAB_TECH','LAB_ADMIN')")
public class ReferenceDataController {

    private final ReferenceDataService service;

    public ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/clients")
    public List<ClientView> clients() {
        return service.listClients();
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LAB_ADMIN')")
    public ClientView createClient(@Valid @RequestBody ClientRequest request) {
        return service.createClient(request);
    }

    @GetMapping("/tests")
    public List<TestView> tests() {
        return service.listTests();
    }

    @PostMapping("/tests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LAB_ADMIN')")
    public TestView createTest(@Valid @RequestBody TestRequest request) {
        return service.createTest(request);
    }
}
