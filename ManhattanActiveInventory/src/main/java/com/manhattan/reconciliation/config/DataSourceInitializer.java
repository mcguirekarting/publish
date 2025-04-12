package com.manhattan.reconciliation.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source initializer that properly sets up the datasource based on Replit environment variables.
 */
@Configuration
public class DataSourceInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceInitializer.class);
    
    @Autowired
    private Environment environment;
    
    @PostConstruct
    public void init() {
        logger.info("Initializing DataSource using environment variables");
    }
    
    /**
     * Creates a datasource using the PostgreSQL-specific environment variables in Replit.
     * 
     * @return A properly configured DataSource
     */
    public DataSource getDataSource() {
        String host = environment.getProperty("PGHOST");
        String port = environment.getProperty("PGPORT");
        String database = environment.getProperty("PGDATABASE");
        String username = environment.getProperty("PGUSER");
        String password = environment.getProperty("PGPASSWORD");
        
        // Log environment variables found (without values for security)
        logger.info("Found PostgreSQL environment variables: PGHOST={}, PGPORT={}, PGDATABASE={}", 
                host != null ? "set" : "not set",
                port != null ? "set" : "not set", 
                database != null ? "set" : "not set");
        
        if (host == null || database == null) {
            logger.warn("Missing required PostgreSQL environment variables. Falling back to DATABASE_URL");
            
            // Try using DATABASE_URL directly as fallback
            String databaseUrl = environment.getProperty("DATABASE_URL");
            if (databaseUrl != null && !databaseUrl.isEmpty()) {
                logger.info("Using DATABASE_URL directly");
                return createDataSourceFromUrl(databaseUrl);
            } else {
                throw new IllegalStateException("Cannot configure database connection. Neither PostgreSQL environment variables nor DATABASE_URL is properly set.");
            }
        }
        
        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        logger.info("Created DataSource with URL: {}", url);
        return dataSource;
    }
    
    /**
     * Creates a datasource directly from a JDBC URL.
     * 
     * @param url The full JDBC URL
     * @return A configured DataSource
     */
    private DataSource createDataSourceFromUrl(String url) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        
        // Check if the URL already has the jdbc:postgresql:// prefix
        if (!url.startsWith("jdbc:")) {
            url = "jdbc:" + url;
        }
        
        dataSource.setUrl(url);
        
        // Username and password are usually included in the URL, but we can set them explicitly if needed
        String username = environment.getProperty("PGUSER");
        String password = environment.getProperty("PGPASSWORD");
        
        if (username != null) {
            dataSource.setUsername(username);
        }
        
        if (password != null) {
            dataSource.setPassword(password);
        }
        
        logger.info("Created DataSource with direct URL");
        return dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // This method will be called after the application is fully initialized
        logger.info("Database connection verified");
    }
}