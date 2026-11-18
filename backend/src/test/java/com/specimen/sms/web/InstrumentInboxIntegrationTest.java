package com.specimen.sms.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.specimen.sms.application.instrument.InstrumentInboxWatcher;
import com.specimen.sms.application.order.OrderService;
import com.specimen.sms.application.specimen.SpecimenService;
import com.specimen.sms.domain.ClientRepository;
import com.specimen.sms.domain.SpecimenStatus;
import com.specimen.sms.domain.TestCatalogRepository;
import com.specimen.sms.web.dto.Dtos.AccessionRequest;
import com.specimen.sms.web.dto.Dtos.CreateOrderRequest;
import com.specimen.sms.web.dto.Dtos.PatientInput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class InstrumentInboxIntegrationTest {

    private static Path inbox;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        inbox = Files.createTempDirectory("sms-inbox");
        registry.add("app.instrument.inbox.enabled", () -> "true");
        registry.add("app.instrument.inbox.directory", () -> inbox.toString());
        registry.add("app.instrument.inbox.poll-ms", () -> "3600000");
    }

    @Autowired InstrumentInboxWatcher watcher;
    @Autowired OrderService orders;
    @Autowired SpecimenService specimens;
    @Autowired ClientRepository clients;
    @Autowired TestCatalogRepository tests;

    @Test
    void inboxFileResultsSpecimenAndBadFilesAreQuarantined() throws Exception {
        var order = orders.create(new CreateOrderRequest(
                clients.findAll().get(0).getId(),
                new PatientInput("MRN-INBOX", "Sam", "Sample", LocalDate.of(1990, 1, 1), "M"),
                List.of(tests.findAll().get(0).getId())));
        var specimen = specimens.accession(order.id(), new AccessionRequest("Gold (SST)", null, true));

        Files.writeString(inbox.resolve("good.json"), """
                {"instrumentId":"FILE-ANALYZER","barcode":"%s",
                 "results":[{"analyte":"TSH","value":"2.1","unit":"mIU/L","flag":"N"}]}
                """.formatted(specimen.barcode()));
        Files.writeString(inbox.resolve("bad.json"), "{\"barcode\":\"UNKNOWN\"}");

        watcher.poll();

        assertThat(specimens.get(specimen.id()).status()).isEqualTo(SpecimenStatus.RESULTED);
        assertThat(inbox.resolve("good.json")).doesNotExist();
        assertThat(inbox.resolve("bad.json")).doesNotExist();
        try (var processed = Files.list(inbox.resolve("processed"))) {
            assertThat(processed.count()).isEqualTo(1);
        }
        try (var failed = Files.list(inbox.resolve("failed"))) {
            assertThat(failed.count()).isEqualTo(2); // file + .error.txt
        }
    }
}
