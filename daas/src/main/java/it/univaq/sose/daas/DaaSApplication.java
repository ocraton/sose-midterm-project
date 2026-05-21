package it.univaq.sose.daas;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

public class DaaSApplication {

    public static void main(String[] args) throws Exception {
        JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();
        
        // Diciamo a CXF quale classe contiene le nostre API REST
        factoryBean.setResourceClasses(CourseResource.class);
        factoryBean.setResourceProvider(CourseResource.class, new SingletonResourceProvider(new CourseResource()));
        
        // Aggiungiamo Jackson per la gestione automatica del JSON
        factoryBean.setProvider(new JacksonJsonProvider());
        
        // Impostiamo l'indirizzo e la porta (0.0.0.0 permette a Docker di esporre la porta correttamente)
        factoryBean.setAddress("http://0.0.0.0:8080/");
        
        System.out.println("Inizializzazione del server DaaS in corso...");
        factoryBean.create();
        System.out.println("Server DaaS avviato con successo e in ascolto sulla porta 8080!");
    }
}
