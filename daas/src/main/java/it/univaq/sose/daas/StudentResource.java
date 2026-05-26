package it.univaq.sose.daas;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.jena.query.ParameterizedSparqlString;

@Path("/api")
public class StudentResource {

    private static final String PREFIXES =
        "PREFIX ex: <http://example.org/uni/> " +
        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ";

    protected List<Map<String, String>> executeSparql(String queryString, String[] vars) {
        return SparqlSupport.executeSparql(queryString, vars, PREFIXES);
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

    private Response badRequest(String message) {
        return Response.status(400).entity("{\"error\":\"" + message + "\"}").build();
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
        if (!SparqlSupport.isValidIdentifier(id)) {
            return badRequest("Parametro id non valido");
        }

        String query = buildStudentByIdQuery(id);
        List<Map<String, String>> res = executeSparql(query, new String[]{"name", "working", "accessibility", "gpa"});
        if (res.isEmpty()) return Response.status(404).entity("{\"error\":\"Studente non trovato\"}").build();
        return Response.ok(res.get(0)).build();
    }
}