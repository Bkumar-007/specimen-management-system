package com.specimen.sms.web;

import com.specimen.sms.application.SpecimenService;
import com.specimen.sms.domain.SpecimenStatus;
import com.specimen.sms.web.dto.Dtos.AddResultsRequest;
import com.specimen.sms.web.dto.Dtos.AuditView;
import com.specimen.sms.web.dto.Dtos.SpecimenPage;
import com.specimen.sms.web.dto.Dtos.SpecimenView;
import com.specimen.sms.web.dto.Dtos.StatsView;
import com.specimen.sms.web.dto.Dtos.StatusChangeRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('LAB_TECH','LAB_ADMIN')")
public class SpecimenController {

    private final SpecimenService specimens;

    public SpecimenController(SpecimenService specimens) {
        this.specimens = specimens;
    }

    /**
     * Search specimens with pagination and filters.
     * {@code needsAction=true} limits to work-queue statuses (COLLECTED…RESULTED).
     */
    @GetMapping("/specimens")
    public SpecimenPage search(
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) SpecimenStatus status,
            @RequestParam(required = false) List<SpecimenStatus> statuses,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "false") boolean needsAction,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int size) {
        return specimens.search(barcode, patient, status, statuses, orderId, from, to, needsAction, page, size);
    }

    @GetMapping("/specimens/{id}")
    public SpecimenView get(@PathVariable Long id) {
        return specimens.get(id);
    }

    @GetMapping("/specimens/{id}/timeline")
    public List<AuditView> timeline(@PathVariable Long id) {
        return specimens.timeline(id);
    }

    @PatchMapping("/specimens/{id}/status")
    public SpecimenView changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return specimens.changeStatus(id, request);
    }

    @PostMapping("/specimens/{id}/results")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecimenView addResults(@PathVariable Long id, @Valid @RequestBody AddResultsRequest request) {
        return specimens.addResults(id, request.version(), request.results());
    }

    @GetMapping("/stats")
    public StatsView stats() {
        return specimens.stats();
    }
}
