package com.specimen.sms.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specimen.sms.web.dto.Dtos.InstrumentMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optional file interface: drop an analyzer message as {@code *.json} into the inbox directory.
 * Processed files move to {@code processed/}, failures to {@code failed/} (with a {@code .error.txt} note).
 * Enable with {@code app.instrument.inbox.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "app.instrument.inbox.enabled", havingValue = "true")
public class InstrumentInboxWatcher {

    private static final Logger log = LoggerFactory.getLogger(InstrumentInboxWatcher.class);

    private final InstrumentService instruments;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final Path inbox;

    public InstrumentInboxWatcher(
            InstrumentService instruments,
            ObjectMapper mapper,
            Validator validator,
            @Value("${app.instrument.inbox.directory:inbox}") String directory) throws IOException {
        this.instruments = instruments;
        this.mapper = mapper;
        this.validator = validator;
        this.inbox = Path.of(directory).toAbsolutePath();
        Files.createDirectories(inbox.resolve("processed"));
        Files.createDirectories(inbox.resolve("failed"));
        log.info("Instrument inbox watching {}", inbox);
    }

    @Scheduled(fixedDelayString = "${app.instrument.inbox.poll-ms:3000}")
    public void poll() {
        try (Stream<Path> files = Files.list(inbox)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .forEach(this::process);
        } catch (IOException e) {
            log.warn("Could not list inbox {}: {}", inbox, e.getMessage());
        }
    }

    private void process(Path file) {
        try {
            InstrumentMessage message = mapper.readValue(file.toFile(), InstrumentMessage.class);
            Set<ConstraintViolation<InstrumentMessage>> violations = validator.validate(message);
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException("Invalid message: " + violations.iterator().next().getMessage());
            }
            var response = instruments.ingest(message);
            log.info("Inbox {} -> specimen {} now {}", file.getFileName(), response.barcode(), response.status());
            move(file, "processed", null);
        } catch (Exception e) {
            log.warn("Inbox file {} failed: {}", file.getFileName(), e.getMessage());
            move(file, "failed", e.getMessage());
        }
    }

    private void move(Path file, String folder, String error) {
        try {
            Path target = inbox.resolve(folder).resolve(System.currentTimeMillis() + "-" + file.getFileName());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            if (error != null) {
                Files.writeString(Path.of(target + ".error.txt"), error);
            }
        } catch (IOException e) {
            log.error("Could not move {}: {}", file, e.getMessage());
        }
    }
}
