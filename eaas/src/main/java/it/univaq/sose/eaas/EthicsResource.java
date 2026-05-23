package it.univaq.sose.eaas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api")
public class EthicsResource {

    // Il percorso all'interno del container Docker (definito nel docker-compose.yml)
    private static final String POLICIES_FILE_PATH = "/app/policies/corsi_policies.json";

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(Map.of("status", "UP")).build();
    }

    @POST
    @Path("/evaluate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response evaluateAssignment(Map<String, Map<String, Object>> payload) {
        try {
            // 1. Estraiamo la "proposta" inviata dal Client
            Map<String, Object> student = payload != null && payload.get("student") != null
                ? payload.get("student")
                : new HashMap<>();
            Map<String, Object> course = payload != null && payload.get("course") != null
                ? payload.get("course")
                : new HashMap<>();

            // 2. Leggiamo il file delle policy esterno
            ObjectMapper mapper = new ObjectMapper();
            File policyFile = new File(POLICIES_FILE_PATH);

            // Fallback locale utile se avvii il progetto fuori da Docker per test
            if (!policyFile.exists()) {
                policyFile = new File("policies/corsi_policies.json");
            }

            JsonNode rootNode = mapper.readTree(policyFile);
            JsonNode policies = rootNode.get("policies");

            if (policies == null || !policies.isArray()) {
                return Response.serverError().entity("{\"error\": \"Configurazione policy non valida\"}").build();
            }

            // 4. Motore di valutazione
            JexlEngine jexl = new JexlBuilder().create();
            JexlContext context = new MapContext();
            context.set("student", student);
            context.set("course", course);

            Map<String, Object> decision = new HashMap<>();
            List<Map<String, Object>> evaluatedPolicies = new ArrayList<>();
            JsonNode appliedPolicy = null;
            boolean hasMatchedNonDefaultPolicy = false;

            for (JsonNode policy : policies) {
                String id = policy.get("id").asText();
                String conditionString = policy.path("condition").asText("DEFAULT");
                boolean conditionMet = false;

                if ("DEFAULT".equals(conditionString)) {
                    conditionMet = !hasMatchedNonDefaultPolicy; // Default solo se nessuna policy di rischio e' stata attivata
                } else {
                    try {
                        JexlExpression expression = jexl.createExpression(conditionString);
                        conditionMet = Boolean.TRUE.equals(expression.evaluate(context));
                    } catch (Exception evalException) {
                        System.err.println("Errore nella valutazione della policy " + id + ": " + evalException.getMessage());
                        conditionMet = false;
                    }
                }

                if (!"DEFAULT".equals(conditionString) && conditionMet) {
                    hasMatchedNonDefaultPolicy = true;
                }

                Map<String, Object> evaluatedPolicy = new HashMap<>();
                evaluatedPolicy.put("policyId", id);
                evaluatedPolicy.put("evaluated", conditionMet);
                evaluatedPolicy.put("condition", conditionString);
                evaluatedPolicy.put("action", policy.get("action").asText());
                evaluatedPolicy.put("riskLevel", policy.get("riskLevel").asText());
                evaluatedPolicies.add(evaluatedPolicy);

                if (conditionMet && appliedPolicy == null) {
                    appliedPolicy = policy;
                }
            }

            if (appliedPolicy == null) {
                return Response.serverError().entity("{\"error\": \"Nessuna policy applicabile trovata\"}").build();
            }

            String auditId = UUID.randomUUID().toString();
            String evaluatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

            String appliedPolicyId = appliedPolicy.get("id").asText();
            String action = appliedPolicy.get("action").asText();
            String riskLevel = appliedPolicy.get("riskLevel").asText();

            // Manteniamo i campi legacy a top-level per retrocompatibilita'
            decision.put("decision", action);
            decision.put("riskLevel", riskLevel);
            decision.put("appliedPolicy", appliedPolicyId);
            decision.put("rationale", appliedPolicy.get("rationale").asText());

            List<String> requiredActions = new ArrayList<>();
            if ("HIGH".equalsIgnoreCase(riskLevel) || "CRITICAL".equalsIgnoreCase(riskLevel)) {
                requiredActions.add("MANUAL_REVIEW_REQUIRED");
            }
            if ("REJECT".equalsIgnoreCase(action)) {
                requiredActions.add("DO_NOT_ASSIGN_COURSE");
            } else if ("ESCALATE".equalsIgnoreCase(action)) {
                requiredActions.add("ASSIGN_TUTOR_ESCALATION");
            } else if ("REVISE".equalsIgnoreCase(action)) {
                requiredActions.add("REVISE_STUDY_PLAN");
            }

            Map<String, Object> inputSnapshot = new HashMap<>();
            inputSnapshot.put("student", student);
            inputSnapshot.put("course", course);

            Map<String, Object> auditTrace = new HashMap<>();
            auditTrace.put("requestId", auditId);
            auditTrace.put("evaluatedAt", evaluatedAt);
            auditTrace.put("inputSnapshot", inputSnapshot);
            auditTrace.put("evaluatedPolicies", evaluatedPolicies);
            auditTrace.put("appliedPolicy", appliedPolicyId);

            decision.put("auditId", auditId);
            decision.put("timestamp", evaluatedAt);
            decision.put("requiredActions", requiredActions);
            decision.put("audit_trace", auditTrace);

            // Persistenza audit in ambiente stateless: Docker conserva stdout/stderr.
            System.out.println("[AUDIT] " + mapper.writeValueAsString(auditTrace));

            // 6. Restituiamo il responso strutturato (esteso in modo additivo)
            return Response.ok(decision).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\": \"Errore interno EaaS: " + e.getMessage() + "\"}").build();
        }
    }
}