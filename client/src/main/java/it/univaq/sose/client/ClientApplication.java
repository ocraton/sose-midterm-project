package it.univaq.sose.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ClientApplication {

    private static final String DAAS_URL = getEnvOrDefault("DAAS_URL", "http://daas:8080/api");
    private static final String EAAS_URL = getEnvOrDefault("EAAS_URL", "http://eaas:8080/api/evaluate");
    private static final int HTTP_MAX_RETRIES = getEnvOrDefaultInt("HTTP_MAX_RETRIES", 3);
    private static final int HTTP_TIMEOUT_MS = getEnvOrDefaultInt("HTTP_TIMEOUT_MS", 3000);
    private static final int HTTP_RETRY_BACKOFF_MS = getEnvOrDefaultInt("HTTP_RETRY_BACKOFF_MS", 500);

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("=== AVVIO CLIENT ORCHESTRATORE ===");
        System.out.println("Configurazione: DAAS_URL=" + DAAS_URL + " | EAAS_URL=" + EAAS_URL);

        try {
            // 1. Chiediamo al DaaS i dati dello Studente (Alice)
            System.out.println("\n1. Richiesta dati studente S001 (Alice) al DaaS...");
            JsonNode student = fetchJson(DAAS_URL + "/students/S001", "student S001");
            System.out.println("Studente recuperato: " + requireTextField(student, "name", "student S001"));

            // 2. Chiediamo al DaaS i dati del Corso (Algoritmi)
            System.out.println("2. Richiesta dati corso C001 (Algoritmi) al DaaS...");
            JsonNode course = fetchJson(DAAS_URL + "/courses/C001", "course C001");
            System.out.println("Corso recuperato: " + requireTextField(course, "title", "course C001"));

            // 3. Normalizziamo il payload con le chiavi attese dall'EaaS
            ObjectNode payload = normalizeForEaas(student, course);
            String jsonPayload = mapper.writeValueAsString(payload);

            // 4. Inviamo la proposta all'EaaS per la valutazione
            System.out.println("\n3. Invio della proposta di assegnazione all'EaaS per valutazione etica...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EAAS_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            String responseBody = sendWithRetry(request, "POST", EAAS_URL, "EaaS evaluation");

            // 5. Leggiamo il Verdetto (Audit Trail)
            JsonNode verdict = mapper.readTree(responseBody);

            System.out.println("\n========================================");
            System.out.println("       VERDETTO EAAS RICEVUTO");
            System.out.println("========================================");
            System.out.println("DECISIONE  : " + safeText(verdict, "decision"));
            System.out.println("RISCHIO    : " + safeText(verdict, "riskLevel"));
            System.out.println("POLICY ID  : " + safeText(verdict, "appliedPolicy"));
            System.out.println("MOTIVAZIONE: " + safeText(verdict, "rationale"));
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("Errore nel flusso client: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static JsonNode fetchJson(String url, String operation) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(HTTP_TIMEOUT_MS))
                .GET()
                .build();
        String body = sendWithRetry(request, "GET", url, operation);
        return mapper.readTree(body);
    }

    private static String sendWithRetry(HttpRequest request, String method, String url, String operation) throws Exception {
        for (int attempt = 1; attempt <= HTTP_MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    return response.body();
                }

                String reason = method + " " + url + " -> HTTP " + statusCode + " body=" + compactBody(response.body());
                if (attempt < HTTP_MAX_RETRIES && isRetryableStatus(statusCode)) {
                    System.err.println("Tentativo " + attempt + "/" + HTTP_MAX_RETRIES + " fallito per " + operation + ": " + reason + ". Riprovo...");
                    waitBeforeRetry(attempt);
                    continue;
                }

                throw new IllegalStateException("Errore funzionale su " + operation + ": " + reason);
            } catch (IOException e) {
                if (attempt < HTTP_MAX_RETRIES) {
                    System.err.println("Tentativo " + attempt + "/" + HTTP_MAX_RETRIES + " fallito per " + operation + ": " + e.getMessage() + ". Riprovo...");
                    waitBeforeRetry(attempt);
                    continue;
                }
                throw new IllegalStateException("Impossibile completare " + operation + " dopo " + HTTP_MAX_RETRIES + " tentativi", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Operazione interrotta durante " + operation, e);
            }
        }

        throw new IllegalStateException("Impossibile completare " + operation + " dopo " + HTTP_MAX_RETRIES + " tentativi");
    }

    private static ObjectNode normalizeForEaas(JsonNode student, JsonNode course) {
        ObjectNode normalizedStudent = mapper.createObjectNode();
        normalizedStudent.put("name", requireTextField(student, "name", "student"));
        normalizedStudent.put("isWorkingStudent", requireTextField(student, "working", "student"));
        normalizedStudent.put("needsAccessibility", requireTextField(student, "accessibility", "student"));
        normalizedStudent.put("gpa", requireTextField(student, "gpa", "student"));

        ObjectNode normalizedCourse = mapper.createObjectNode();
        normalizedCourse.put("title", requireTextField(course, "title", "course"));
        normalizedCourse.put("workload", requireTextField(course, "workload", "course"));
        normalizedCourse.put("requiresPhysicalPresence", requireTextField(course, "physical", "course"));

        ObjectNode payload = mapper.createObjectNode();
        payload.set("student", normalizedStudent);
        payload.set("course", normalizedCourse);
        return payload;
    }

    private static String requireTextField(JsonNode node, String field, String sourceName) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Payload incompleto da " + sourceName + ": campo obbligatorio mancante '" + field + "'");
        }
        return value.asText();
    }

    private static String safeText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "N/A";
        }
        String text = value.asText();
        return text.isBlank() ? "N/A" : text;
    }

    private static boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private static void waitBeforeRetry(int attempt) throws InterruptedException {
        long backoff = (long) HTTP_RETRY_BACKOFF_MS * attempt;
        Thread.sleep(backoff);
    }

    private static String compactBody(String body) {
        if (body == null) {
            return "<empty>";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 180) {
            return compact;
        }
        return compact.substring(0, 180) + "...";
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static int getEnvOrDefaultInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Valore env non valido per " + key + "='" + value + "', uso default=" + defaultValue);
            return defaultValue;
        }
    }
}