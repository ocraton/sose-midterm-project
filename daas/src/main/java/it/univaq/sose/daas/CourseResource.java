package it.univaq.sose.daas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.jena.query.QueryExecution;
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
    private List<Map<String, String>> executeSparql(String queryString, String[] vars) {
        List<Map<String, String>> list = new ArrayList<>();
        try (QueryExecution qexec = QueryExecution.service(SPARQL_ENDPOINT).query(PREFIXES + queryString).build()) {
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
        String query = "SELECT ?title ?workload ?capacity ?prereq ?physical WHERE { " +
                       "?course a ex:Course ; ex:courseId \"" + id + "\" ; ex:title ?title ; " +
                       "ex:workload ?workload ; ex:capacity ?capacity ; " +
                       "ex:hasPrerequisite ?prereq ; ex:requiresPhysicalPresence ?physical . }";
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
        String query = "SELECT ?name ?working ?accessibility ?gpa WHERE { " +
                       "?s a foaf:Person ; ex:studentId \"" + id + "\" ; foaf:name ?name ; " +
                       "ex:isWorkingStudent ?working ; ex:needsAccessibility ?accessibility ; ex:gpa ?gpa . }";
        List<Map<String, String>> res = executeSparql(query, new String[]{"name", "working", "accessibility", "gpa"});
        if (res.isEmpty()) return Response.status(404).entity("{\"error\":\"Studente non trovato\"}").build();
        return Response.ok(res.get(0)).build();
    }

    // 5. GET /api/courses/search (Query complessa con condizioni multiple)
    @GET
    @Path("/courses/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCourses(@QueryParam("workload") String workload, @QueryParam("physical") String physical) {
        String query = "SELECT ?id ?title ?workload ?physical WHERE { " +
                       "?course a ex:Course ; ex:courseId ?id ; ex:title ?title ; " +
                       "ex:workload ?workload ; ex:requiresPhysicalPresence ?physical . ";
        
        // Aggiungiamo i filtri SPARQL solo se l'utente li ha richiesti nell'URL
        if (workload != null && !workload.isEmpty()) {
            query += "FILTER(?workload = \"" + workload + "\") ";
        }
        if (physical != null && !physical.isEmpty()) {
            query += "FILTER(?physical = " + physical + ") ";
        }
        query += "}";
        
        return Response.ok(executeSparql(query, new String[]{"id", "title", "workload", "physical"})).build();
    }
}
