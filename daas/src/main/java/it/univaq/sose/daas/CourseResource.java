package it.univaq.sose.daas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

@Path("/api")
public class CourseResource {

    private static final String SPARQL_ENDPOINT = "http://triplestore:3030/dataset/query";
    private static final String PREFIXES = 
        "PREFIX ex: <http://example.org/uni/> " +
        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ";

    // Metodo di utilità: esegue la query e formatta i risultati per evitare di ripetere codice
    protected List<Map<String, String>> executeSparql(String queryString, String[] vars) {
        List<Map<String, String>> list = new ArrayList<>();
        String query = queryString.startsWith("PREFIX ") ? queryString : PREFIXES + queryString;
        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(query).build()) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Map<String, String> map = new HashMap<>();
                for (String var : vars) {
                    if (soln.contains(var)) {
                        RDFNode node = soln.get(var);
                        // Estraiamo il valore pulito (senza i tipi di dato SPARQL come ^^xsd:boolean)
                        map.put(var, node.isLiteral() ? node.asLiteral().getString() : node.toString());
                    }
                }
                list.add(map);
            }
        }
        return list;
    }

    static boolean isValidIdentifier(String value) {
        return value != null && !value.isBlank() && value.matches("[A-Za-z0-9_-]+");
    }

    static boolean isValidWorkload(String value) {
        return value != null && ("High".equalsIgnoreCase(value) || "Low".equalsIgnoreCase(value));
    }

    static Boolean parsePhysicalFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new BadRequestException("Parametro physical non valido");
    }

    static String buildCourseByIdQuery(String id) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(PREFIXES +
                "SELECT ?title ?workload ?capacity ?prereq ?physical WHERE { " +
                "?course a ex:Course ; ex:courseId ?courseId ; ex:title ?title ; " +
                "ex:workload ?workload ; ex:capacity ?capacity ; " +
                "ex:hasPrerequisite ?prereq ; ex:requiresPhysicalPresence ?physical . " +
                "FILTER(?courseId = ?courseIdFilter) }");
        query.setLiteral("courseIdFilter", id);
        return query.toString();
    }

    static String buildStudentByIdQuery(String id) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(PREFIXES +
                "SELECT ?name ?working ?accessibility ?gpa WHERE { " +
                "?s a foaf:Person ; ex:studentId ?studentId ; foaf:name ?name ; " +
                "ex:isWorkingStudent ?working ; ex:needsAccessibility ?accessibility ; ex:gpa ?gpa . " +
                "FILTER(?studentId = ?studentIdFilter) }");
        query.setLiteral("studentIdFilter", id);
        return query.toString();
    }

    static String buildSearchCoursesQuery(String workload, Boolean physical) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(PREFIXES +
                "SELECT ?id ?title ?workload ?physical WHERE { " +
                "?course a ex:Course ; ex:courseId ?id ; ex:title ?title ; " +
                "ex:workload ?workload ; ex:requiresPhysicalPresence ?physical . ");

        if (workload != null && !workload.isBlank()) {
            query.append("FILTER(?workload = ?workloadFilter) ");
            query.setLiteral("workloadFilter", workload);
        }

        if (physical != null) {
            query.append("FILTER(?physical = ?physicalFilter) ");
            query.setLiteral("physicalFilter", physical);
        }

        query.append("}");
        return query.toString();
    }

    private Response badRequest(String message) {
        return Response.status(400).entity("{\"error\":\"" + message + "\"}").build();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(Map.of("status", "UP")).build();
    }

    // 1. GET /api/courses
    @GET
    @Path("/courses")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCourses() {
        String query = "SELECT ?id ?title ?workload ?capacity ?prereq ?physical WHERE { " +
                       "?course a ex:Course ; ex:courseId ?id ; ex:title ?title ; " +
                       "ex:workload ?workload ; ex:capacity ?capacity ; " +
                       "ex:hasPrerequisite ?prereq ; ex:requiresPhysicalPresence ?physical . }";
        return Response.ok(executeSparql(query, new String[]{"id", "title", "workload", "capacity", "prereq", "physical"})).build();
    }

    // 2. GET /api/courses/{id}
    @GET
    @Path("/courses/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCourseById(@PathParam("id") String id) {
        if (!isValidIdentifier(id)) {
            return badRequest("Parametro id non valido");
        }

        String query = buildCourseByIdQuery(id);
        List<Map<String, String>> res = executeSparql(query, new String[]{"title", "workload", "capacity", "prereq", "physical"});
        if (res.isEmpty()) return Response.status(404).entity("{\"error\":\"Corso non trovato\"}").build();
        return Response.ok(res.get(0)).build();
    }

    // 3. GET /api/students
    @GET
    @Path("/students")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudents() {
        String query = "SELECT ?id ?name ?working ?accessibility ?gpa WHERE { " +
                       "?s a foaf:Person ; ex:studentId ?id ; foaf:name ?name ; " +
                       "ex:isWorkingStudent ?working ; ex:needsAccessibility ?accessibility ; ex:gpa ?gpa . }";
        return Response.ok(executeSparql(query, new String[]{"id", "name", "working", "accessibility", "gpa"})).build();
    }

    // 4. GET /api/students/{id}
    @GET
    @Path("/students/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudentById(@PathParam("id") String id) {
        if (!isValidIdentifier(id)) {
            return badRequest("Parametro id non valido");
        }

        String query = buildStudentByIdQuery(id);
        List<Map<String, String>> res = executeSparql(query, new String[]{"name", "working", "accessibility", "gpa"});
        if (res.isEmpty()) return Response.status(404).entity("{\"error\":\"Studente non trovato\"}").build();
        return Response.ok(res.get(0)).build();
    }

    // 5. GET /api/courses/search (Query complessa con condizioni multiple)
    @GET
    @Path("/courses/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCourses(@QueryParam("workload") String workload, @QueryParam("physical") String physical) {
        if (workload != null && !workload.isBlank() && !isValidWorkload(workload)) {
            return badRequest("Parametro workload non valido");
        }

        Boolean physicalFilter;
        try {
            physicalFilter = parsePhysicalFilter(physical);
        } catch (BadRequestException ex) {
            return badRequest(ex.getMessage());
        }

        String query = buildSearchCoursesQuery(workload, physicalFilter);
        return Response.ok(executeSparql(query, new String[]{"id", "title", "workload", "physical"})).build();
    }
}
