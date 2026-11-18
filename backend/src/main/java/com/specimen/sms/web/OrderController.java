package com.specimen.sms.web;

import com.specimen.sms.application.order.OrderService;
import com.specimen.sms.application.specimen.SpecimenService;
import com.specimen.sms.domain.SpecimenStatus;
import com.specimen.sms.web.dto.Dtos.AccessionRequest;
import com.specimen.sms.web.dto.Dtos.AuditView;
import com.specimen.sms.web.dto.Dtos.CreateOrderRequest;
import com.specimen.sms.web.dto.Dtos.OrderDetail;
import com.specimen.sms.web.dto.Dtos.OrderSummary;
import com.specimen.sms.web.dto.Dtos.SpecimenView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('LAB_TECH','LAB_ADMIN')")
public class OrderController {

    private final OrderService orders;
    private final SpecimenService specimens;

    public OrderController(OrderService orders, SpecimenService specimens) {
        this.orders = orders;
        this.specimens = specimens;
    }

    @GetMapping
    public List<OrderSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SpecimenStatus status) {
        return orders.search(q, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetail create(@Valid @RequestBody CreateOrderRequest request) {
        return orders.create(request);
    }

    @GetMapping("/{id}")
    public OrderDetail get(@PathVariable Long id) {
        return orders.get(id);
    }

    @GetMapping("/{id}/timeline")
    public List<AuditView> timeline(@PathVariable Long id) {
        return orders.timeline(id);
    }

    /** Removes an order only when it has no specimens. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orders.delete(id);
    }

    @PostMapping("/{id}/specimens")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecimenView accession(@PathVariable Long id, @Valid @RequestBody AccessionRequest request) {
        return specimens.accession(id, request);
    }
}
