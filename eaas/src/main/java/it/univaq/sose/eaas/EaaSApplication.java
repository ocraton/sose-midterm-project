package it.univaq.sose.eaas;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

public class EaaSApplication {

    public static void main(String[] args) throws Exception {
        JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();

        factoryBean.setResourceClasses(EthicsResource.class);
        factoryBean.setResourceProvider(EthicsResource.class, new SingletonResourceProvider(new EthicsResource()));
        factoryBean.setProvider(new JacksonJsonProvider());

        factoryBean.setAddress("http://0.0.0.0:8080/");

        System.out.println("Inizializzazione del motore EaaS in corso...");
        factoryBean.create();
        System.out.println("Server EaaS avviato e in ascolto sulla porta 8080!");
    }
}