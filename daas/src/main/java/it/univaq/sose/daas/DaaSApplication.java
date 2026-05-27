package it.univaq.sose.daas;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

public class DaaSApplication {

    public static void main(String[] args) throws Exception {
        JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();
        
        // Registriamo risorse separate per health, corsi e studenti
        factoryBean.setResourceClasses(
            HealthResource.class,
            CourseResource.class,
            StudentResource.class,
            SwaggerDocsResource.class
        );
        factoryBean.setResourceProvider(HealthResource.class, new SingletonResourceProvider(new HealthResource()));
        factoryBean.setResourceProvider(CourseResource.class, new SingletonResourceProvider(new CourseResource()));
        factoryBean.setResourceProvider(StudentResource.class, new SingletonResourceProvider(new StudentResource()));
        factoryBean.setResourceProvider(SwaggerDocsResource.class, new SingletonResourceProvider(new SwaggerDocsResource()));
        
        // Aggiungiamo Jackson per la gestione automatica del JSON
        factoryBean.setProvider(new JacksonJsonProvider());

        // Attiviamo OpenAPI/Swagger UI per la documentazione interattiva delle API
        OpenApiFeature openApiFeature = new OpenApiFeature();
        openApiFeature.setSupportSwaggerUi(true);
        openApiFeature.setTitle("DaaS API - SOSE Midterm");
        openApiFeature.setDescription("API REST per l'estrazione dati dal Triplestore RDF");
        openApiFeature.setVersion("1.0.0");
        factoryBean.getFeatures().add(openApiFeature);
        
        // Impostiamo l'indirizzo e la porta (0.0.0.0 permette a Docker di esporre la porta correttamente)
        factoryBean.setAddress("http://0.0.0.0:8080/");
        
        System.out.println("Inizializzazione del server DaaS in corso...");
        factoryBean.create();
        System.out.println("Server DaaS avviato con successo e in ascolto sulla porta 8080!");
        System.out.println("Swagger UI disponibile all'indirizzo: http://localhost:8081/api/docs");
    }
}
