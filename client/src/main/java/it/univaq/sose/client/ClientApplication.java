package it.univaq.sose.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClientApplication {

    // Usiamo i nomi dei container Docker come hostname
    private static final String DAAS_URL = "http://daas:8080/api";
    private static final String EAAS_URL = "http://eaas:8080/api/evaluate";

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("=== AVVIO CLIENT ORCHESTRATORE ===");

        try {
            // Aspettiamo 5 secondi per dare tempo a DaaS ed EaaS di avviarsi nel container
            Thread.sleep(5000);

            // 1. Chiediamo al DaaS i dati dello Studente (Alice)
            System.out.println("\n1. Richiesta dati studente S001 (Alice) al DaaS...");
            JsonNode student = fetchJson(DAAS_URL + "/students/S001");
            System.out.println("Studente recuperato: " + student.get("name").asText());

            // 2. Chiediamo al DaaS i dati del Corso (Algoritmi)
            System.out.println("2. Richiesta dati corso C001 (Algoritmi) al DaaS...");
            JsonNode course = fetchJson(DAAS_URL + "/courses/C001");
            System.out.println("Corso recuperato: " + course.get("title").asText());

            // 3. Normalizziamo il payload con le chiavi attese dall'EaaS
            ObjectNode normalizedStudent = mapper.createObjectNode();
            normalizedStudent.put("name", student.path("name").asText());
            normalizedStudent.put("isWorkingStudent", student.path("working").asText());
            normalizedStudent.put("needsAccessibility", student.path("accessibility").asText());
            normalizedStudent.put("gpa", student.path("gpa").asText());

            ObjectNode normalizedCourse = mapper.createObjectNode();
            normalizedCourse.put("title", course.path("title").asText());
            normalizedCourse.put("workload", course.path("workload").asText());
            normalizedCourse.put("requiresPhysicalPresence", course.path("physical").asText());

            ObjectNode payload = mapper.createObjectNode();
            payload.set("student", normalizedStudent);
            payload.set("course", normalizedCourse);
            String jsonPayload = mapper.writeValueAsString(payload);

            // 4. Inviamo la proposta all'EaaS per la valutazione
            System.out.println("\n3. Invio della proposta di assegnazione all'EaaS per valutazione etica...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EAAS_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Leggiamo il Verdetto (Audit Trail)
            JsonNode verdict = mapper.readTree(response.body());

            System.out.println("\n========================================");
            System.out.println("       VERDETTO EAAS RICEVUTO");
            System.out.println("========================================");
            System.out.println("DECISIONE  : " + verdict.get("decision").asText());
            System.out.println("RISCHIO    : " + verdict.get("riskLevel").asText());
            System.out.println("POLICY ID  : " + verdict.get("appliedPolicy").asText());
            System.out.println("MOTIVAZIONE: " + verdict.get("rationale").asText());
            System.out.println("========================================\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metodo helper per le GET al DaaS
    private static JsonNode fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body());
    }
}