package com.specimen.sms.web;

import com.specimen.sms.application.instrument.InstrumentService;
import com.specimen.sms.security.CurrentUser;
import com.specimen.sms.web.dto.Dtos.IngestResponse;
import com.specimen.sms.web.dto.Dtos.InstrumentMessage;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulated analyzer interface. Callers authenticate either with a normal JWT (lab user) or with the
 * {@code X-Instrument-Key} header, which lets a script or "instrument" post without a user login.
 */
@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instruments;
    private final String apiKey;

    public InstrumentController(InstrumentService instruments, @Value("${app.instrument.api-key}") String apiKey) {
        this.instruments = instruments;
        this.apiKey = apiKey;
    }

    @PostMapping("/ingest")
    public IngestResponse ingest(
            @RequestHeader(value = "X-Instrument-Key", required = false) String key,
            @Valid @RequestBody InstrumentMessage message) {
        boolean keyOk = key != null
                && MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8));
        if (!keyOk && !CurrentUser.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Instrument key or login required");
        }
        return instruments.ingest(message);
    }
}
