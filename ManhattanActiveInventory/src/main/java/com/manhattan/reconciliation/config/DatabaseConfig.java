package com.manhattan.reconciliation.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Database configuration class that provides the data source.
 */
@Configuration
public class DatabaseConfig {

    @Autowired
    private DataSourceInitializer dataSourceInitializer;
    
    /**
     * Creates the datasource bean using the datasource initializer.
     * 
     * @return A properly configured DataSource
     */
    @Bean
    public DataSource dataSource() {
        return dataSourceInitializer.getDataSource();
    }
}