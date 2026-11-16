package com.specimen.sms.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class SpecimenWorkflowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String login(String user, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private JsonNode read(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private long createOrder(String tech, String mrn) throws Exception {
        long clientId = read(mvc.perform(get("/api/clients").header("Authorization", tech))).get(0).get("id").asLong();
        JsonNode tests = read(mvc.perform(get("/api/tests").header("Authorization", tech)));
        long testId = tests.get(0).get("id").asLong();
        String body = """
                {"clientId":%d,"testIds":[%d],
                 "patient":{"mrn":"%s","firstName":"Jane","lastName":"Doe","dateOfBirth":"1985-04-12","sex":"F"}}
                """.formatted(clientId, testId, mrn);
        return read(mvc.perform(post("/api/orders").header("Authorization", tech)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ORDERED"))).get("id").asLong();
    }

    private JsonNode accession(String tech, long orderId) throws Exception {
        return read(mvc.perform(post("/api/orders/" + orderId + "/specimens").header("Authorization", tech)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tubeType\":\"Lavender (EDTA)\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.barcode").value(org.hamcrest.Matchers.startsWith("SPC"))));
    }

    private ResultActions setStatus(String auth, long specimenId, String statusJson) throws Exception {
        return mvc.perform(patch("/api/specimens/" + specimenId + "/status").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(statusJson));
    }

    @Test
    void happyPathFromOrderToReportedWithAudit() throws Exception {
        String tech = login("tech", "tech123");
        String admin = login("admin", "admin123");

        long orderId = createOrder(tech, "MRN-1001");
        JsonNode specimen = accession(tech, orderId);
        long id = specimen.get("id").asLong();
        String barcode = specimen.get("barcode").asText();

        setStatus(tech, id, "{\"status\":\"IN_TESTING\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_TESTING"));

        // cannot report without results
        setStatus(tech, id, "{\"status\":\"REPORTED\"}").andExpect(status().isConflict());

        mvc.perform(post("/api/specimens/" + id + "/results").header("Authorization", tech)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"results\":[{\"analyte\":\"WBC\",\"value\":\"11.9\",\"unit\":\"10^3/uL\",\"flagged\":true},"
                                + "{\"analyte\":\"HGB\",\"value\":\"13.8\",\"unit\":\"g/dL\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESULTED"))
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].flagged").value(true));

        // results cannot be entered twice
        mvc.perform(post("/api/specimens/" + id + "/results").header("Authorization", tech)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"results\":[{\"analyte\":\"PLT\",\"value\":\"250\"}]}"))
                .andExpect(status().isConflict());

        setStatus(tech, id, "{\"status\":\"REPORTED\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REPORTED"));

        // terminal: no further change
        setStatus(admin, id, "{\"status\":\"CANCELLED\",\"reason\":\"too late\"}").andExpect(status().isConflict());

        mvc.perform(get("/api/specimens").param("barcode", barcode).header("Authorization", tech))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/specimens").param("patient", "mrn-1001").param("status", "REPORTED")
                        .header("Authorization", tech))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(get("/api/specimens/" + id + "/timeline").header("Authorization", tech))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("ACCESSIONED"))
                .andExpect(jsonPath("$[-1].details").value("RESULTED -> REPORTED"));

        mvc.perform(get("/api/audit").param("entityType", "SPECIMEN").param("entityId", String.valueOf(id))
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", tech))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.specimens.length()").value(1));
    }

    @Test
    void illegalJumpAndRoleChecks() throws Exception {
        String tech = login("tech", "tech123");
        String admin = login("admin", "admin123");

        long orderId = createOrder(tech, "MRN-2002");
        long id = accession(tech, orderId).get("id").asLong();

        setStatus(tech, id, "{\"status\":\"REPORTED\"}").andExpect(status().isConflict());
        setStatus(tech, id, "{\"status\":\"RESULTED\"}").andExpect(status().isConflict());
        setStatus(tech, id, "{\"status\":\"REJECTED\",\"reason\":\"hemolyzed\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("admin")));
        setStatus(admin, id, "{\"status\":\"REJECTED\"}").andExpect(status().isConflict()); // reason needed
        setStatus(admin, id, "{\"status\":\"REJECTED\",\"reason\":\"hemolyzed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.statusReason").value("hemolyzed"));

        mvc.perform(get("/api/audit").header("Authorization", tech)).andExpect(status().isForbidden());
        mvc.perform(post("/api/clients").header("Authorization", tech).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X1\",\"name\":\"Nope\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/clients").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X1\",\"name\":\"New Clinic\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void validationReturnsFieldErrors() throws Exception {
        String tech = login("tech", "tech123");
        mvc.perform(post("/api/orders").header("Authorization", tech).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"testIds\":[],\"patient\":{\"mrn\":\"\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void instrumentIngestPostsResultsByBarcode() throws Exception {
        String tech = login("tech", "tech123");
        long orderId = createOrder(tech, "MRN-3003");
        JsonNode specimen = accession(tech, orderId);
        String barcode = specimen.get("barcode").asText();
        String message = """
                {"instrumentId":"SIM-ANALYZER-1","barcode":"%s",
                 "results":[{"analyte":"TSH","value":"6.2","unit":"mIU/L","flag":"H"},
                            {"analyte":"FT4","value":"1.1","unit":"ng/dL","flag":"N"}]}
                """.formatted(barcode);

        mvc.perform(post("/api/instruments/ingest").contentType(MediaType.APPLICATION_JSON).content(message))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/instruments/ingest").header("X-Instrument-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(message))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/instruments/ingest").header("X-Instrument-Key", "test-instrument-key")
                        .contentType(MediaType.APPLICATION_JSON).content(message))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESULTED"))
                .andExpect(jsonPath("$.resultsPosted").value(2));

        mvc.perform(get("/api/specimens/" + specimen.get("id").asLong()).header("Authorization", tech))
                .andExpect(jsonPath("$.results[0].flagged").value(true))
                .andExpect(jsonPath("$.results[1].flagged").value(false))
                .andExpect(jsonPath("$.results[0].enteredBy").value("instrument:SIM-ANALYZER-1"));

        // unknown barcode
        mvc.perform(post("/api/instruments/ingest").header("X-Instrument-Key", "test-instrument-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instrumentId\":\"X\",\"barcode\":\"NOPE\",\"results\":[{\"analyte\":\"A\",\"value\":\"1\"}]}"))
                .andExpect(status().isNotFound());
    }
}
