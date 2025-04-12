package com.manhattan.reconciliation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the RestTemplate used by client classes.
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * Creates a RestTemplate bean with a reasonable timeout configuration.
     * 
     * @return A configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(10000);   // 10 seconds
        
        return new RestTemplate(factory);
    }
}