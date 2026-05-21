package it.univaq.sose.daas;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api")
public class CourseResource {

    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping() {
        // Un semplice JSON di test per assicurarci che il server funzioni
        String jsonResponse = "{\"status\": \"DaaS is running!\", \"message\": \"Ready to query Fuseki\"}";
        return Response.ok(jsonResponse).build();
    }
}
