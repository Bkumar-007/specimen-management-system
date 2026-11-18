package com.specimen.sms.web.dto;

import com.specimen.sms.domain.SpecimenStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request/response shapes for the REST API. */
public final class Dtos {

    private Dtos() {
    }

    // ---- reference data ----

    public record ClientRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 128) String name) {
    }

    public record ClientView(Long id, String code, String name) {
    }

    public record TestRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 128) String name,
            @NotBlank @Size(max = 64) String tubeType) {
    }

    public record TestView(Long id, String code, String name, String tubeType, boolean active) {
    }

    // ---- orders ----

    public record PatientInput(
            @NotBlank @Size(max = 32) String mrn,
            @NotBlank @Size(max = 64) String firstName,
            @NotBlank @Size(max = 64) String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            @Size(max = 16) String sex) {
    }

    public record CreateOrderRequest(
            @NotNull Long clientId,
            @NotNull @Valid PatientInput patient,
            @NotEmpty List<Long> testIds) {
    }

    public record PatientView(Long id, String mrn, String firstName, String lastName, LocalDate dateOfBirth, String sex) {
    }

    public record OrderSummary(
            Long id,
            String orderNumber,
            SpecimenStatus status,
            String clientName,
            String patientName,
            String mrn,
            List<String> testCodes,
            int specimenCount,
            Instant createdAt) {
    }

    public record OrderDetail(
            Long id,
            String orderNumber,
            SpecimenStatus status,
            ClientView client,
            PatientView patient,
            List<TestView> tests,
            List<SpecimenView> specimens,
            Instant createdAt,
            String createdBy) {
    }

    // ---- specimens ----

    public record AccessionRequest(
            @NotBlank @Size(max = 64) String tubeType,
            Instant collectedAt,
            /** Defaults to true: mark received at accession time. When false the specimen stays COLLECTED. */
            Boolean markReceived) {
    }

    public record StatusChangeRequest(
            @NotNull SpecimenStatus status,
            @NotNull Long version,
            @Size(max = 500) String reason,
            Boolean override) {
    }

    public record ResultInput(
            @NotBlank @Size(max = 64) String analyte,
            @NotBlank @Size(max = 128) String value,
            @Size(max = 32) String unit,
            Boolean flagged) {
    }

    public record AddResultsRequest(
            @NotNull Long version,
            @NotEmpty List<@Valid ResultInput> results) {
    }

    public record ResultView(
            Long id, String analyte, String value, String unit, boolean flagged, Instant resultedAt, String enteredBy) {
    }

    public record SpecimenView(
            Long id,
            String barcode,
            Long orderId,
            String orderNumber,
            String patientName,
            String mrn,
            String tubeType,
            Instant collectedAt,
            Instant receivedAt,
            SpecimenStatus status,
            String statusReason,
            List<SpecimenStatus> allowedNext,
            List<ResultView> results,
            Long version,
            Instant createdAt) {
    }

    // ---- audit ----

    public record AuditView(
            Long id, String entityType, Long entityId, String action, String details, String performedBy, Instant performedAt) {
    }

    // ---- instrument interface ----

    public record InstrumentResult(
            @NotBlank String analyte,
            @NotBlank String value,
            String unit,
            Boolean flagged,
            /** Analyzer style flag: H, L, HH, LL, A (abnormal) mark the result flagged; N or blank do not. */
            String flag) {
    }

    public record InstrumentMessage(
            @NotBlank String instrumentId,
            @NotBlank String barcode,
            @NotEmpty List<@Valid InstrumentResult> results) {
    }

    public record IngestResponse(String barcode, Long specimenId, SpecimenStatus status, int resultsPosted) {
    }

    // ---- misc ----

    public record StatsView(Map<String, Long> bySpecimenStatus, long totalOrders, long totalSpecimens) {
    }
}
