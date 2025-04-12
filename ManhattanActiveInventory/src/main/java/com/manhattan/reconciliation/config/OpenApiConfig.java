package com.manhattan.reconciliation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI documentation.
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port}")
    private String serverPort;
    
    /**
     * Configures the OpenAPI documentation.
     *
     * @return The OpenAPI configuration
     */
    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Development server");
        
        Contact contact = new Contact();
        contact.setName("Manhattan Associates");
        contact.setEmail("support@manh.com");
        contact.setUrl("https://www.manh.com");
        
        License license = new License()
                .name("Proprietary")
                .url("https://www.manh.com/license");
        
        Info info = new Info()
                .title("Inventory Reconciliation Service API")
                .version("1.0.0")
                .description("This API provides operations for reconciling inventory between Manhattan Active Omni and Manhattan Active Warehouse Management systems.")
                .contact(contact)
                .license(license);
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
}