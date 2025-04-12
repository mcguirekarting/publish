package com.manhattan.reconciliation.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Database initializer that ensures the schema is properly set up.
 */
@Configuration
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Initialize the database by executing schema.sql.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing database schema");
        try {
            ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(false, false, "UTF-8");
            resourceDatabasePopulator.addScript(new ClassPathResource("schema.sql"));
            resourceDatabasePopulator.execute(dataSource);
            logger.info("Database schema initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
        }
    }

    /**
     * Command line runner to verify database connection and schema.
     */
    @Bean
    public CommandLineRunner checkDatabaseSetup(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                logger.info("Verifying database setup...");
                // Check if the reconciliation_result table exists
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'reconciliation_result'", 
                        Integer.class);
                
                if (count != null && count > 0) {
                    logger.info("Database schema verification successful. Table 'reconciliation_result' exists.");
                } else {
                    logger.warn("Table 'reconciliation_result' not found. Schema may not be properly initialized.");
                }
            } catch (Exception e) {
                logger.error("Error verifying database setup", e);
            }
        };
    }
}