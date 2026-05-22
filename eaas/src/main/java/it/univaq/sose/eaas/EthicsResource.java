package it.univaq.sose.eaas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Path("/api")
public class EthicsResource {

    // Il percorso all'interno del container Docker (definito nel docker-compose.yml)
    private static final String POLICIES_FILE_PATH = "/app/policies/corsi_policies.json";

    @POST
    @Path("/evaluate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response evaluateAssignment(Map<String, Map<String, Object>> payload) {
        try {
            // 1. Estraiamo la "proposta" inviata dal Client
            Map<String, Object> student = payload.get("student");
            Map<String, Object> course = payload.get("course");

            // 2. Leggiamo il file delle policy esterno
            ObjectMapper mapper = new ObjectMapper();
            File policyFile = new File(POLICIES_FILE_PATH);

            // Fallback locale utile se avvii il progetto fuori da Docker per test
            if (!policyFile.exists()) {
                policyFile = new File("policies/corsi_policies.json");
            }

            JsonNode rootNode = mapper.readTree(policyFile);
            JsonNode policies = rootNode.get("policies");

            // 3. Estraiamo le variabili da valutare (con conversioni sicure)
            boolean needsAccessibility = Boolean.parseBoolean(String.valueOf(student.get("needsAccessibility")));
            boolean isWorkingStudent = Boolean.parseBoolean(String.valueOf(student.get("isWorkingStudent")));
            double gpa = Double.parseDouble(String.valueOf(student.get("gpa")));

            boolean requiresPhysicalPresence = Boolean.parseBoolean(String.valueOf(course.get("requiresPhysicalPresence")));
            String workload = String.valueOf(course.get("workload"));

            // 4. Motore di valutazione
            Map<String, String> decision = new HashMap<>();

            for (JsonNode policy : policies) {
                String id = policy.get("id").asText();
                boolean conditionMet = false;

                // Valutiamo le condizioni esatte richieste
                if (id.equals("POL-001") && needsAccessibility && requiresPhysicalPresence) {
                    conditionMet = true;
                } else if (id.equals("POL-003") && (gpa < 20.0) && "High".equals(workload)) {
                    conditionMet = true;
                } else if (id.equals("POL-002") && isWorkingStudent && "High".equals(workload)) {
                    conditionMet = true;
                } else if (id.equals("POL-004")) {
                    conditionMet = true; // Default: si innesca se le regole sopra sono tutte false
                }

                // 5. Se la condizione e soddisfatta, registriamo l'Audit Trail e interrompiamo il ciclo
                if (conditionMet) {
                    decision.put("decision", policy.get("action").asText());
                    decision.put("riskLevel", policy.get("riskLevel").asText());
                    decision.put("appliedPolicy", id);
                    decision.put("rationale", policy.get("rationale").asText());
                    break;
                }
            }

            // 6. Restituiamo il responso strutturato
            return Response.ok(decision).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"Errore interno EaaS: " + e.getMessage() + "\"}").build();
        }
    }
}