package com.specimen.sms.application.instrument;

import com.specimen.sms.application.specimen.SpecimenService;
import com.specimen.sms.web.dto.Dtos.IngestResponse;
import com.specimen.sms.web.dto.Dtos.InstrumentMessage;
import com.specimen.sms.web.dto.Dtos.InstrumentResult;
import com.specimen.sms.web.dto.Dtos.ResultInput;
import com.specimen.sms.web.dto.Dtos.SpecimenView;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Simulated lab instrument interface: a JSON message from an "analyzer" is matched to a specimen
 * by barcode and its results are posted, moving the specimen to RESULTED.
 */
@Service
public class InstrumentService {

    private static final Set<String> NORMAL_FLAGS = Set.of("", "N");

    private final SpecimenService specimens;

    public InstrumentService(SpecimenService specimens) {
        this.specimens = specimens;
    }

    public IngestResponse ingest(InstrumentMessage message) {
        Long specimenId = specimens.findIdByBarcode(message.barcode());
        List<ResultInput> inputs = message.results().stream().map(InstrumentService::toInput).toList();
        SpecimenView view = specimens.addResults(specimenId, inputs, "instrument:" + message.instrumentId().trim());
        return new IngestResponse(view.barcode(), view.id(), view.status(), inputs.size());
    }

    private static ResultInput toInput(InstrumentResult r) {
        String flag = r.flag() == null ? "" : r.flag().trim().toUpperCase();
        boolean flagged = Boolean.TRUE.equals(r.flagged()) || !NORMAL_FLAGS.contains(flag);
        return new ResultInput(r.analyte(), r.value(), r.unit(), flagged);
    }
}
