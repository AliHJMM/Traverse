package com.traverse.travel.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * JPA (Postgres) and Neo4j each need their own PlatformTransactionManager,
 * and Spring Boot's own Neo4j auto-configuration backs off entirely once it
 * sees another transaction manager on the classpath -- without this,
 * Neo4j repository calls fail with a NullPointerException deep in Spring
 * Data's internal TransactionTemplate plumbing. Marking the JPA one
 * @Primary keeps every *unqualified* @Transactional (the vast majority of
 * the codebase, including tests) resolving to Postgres as before; only
 * DestinationGraphService opts into "neo4jTransactionManager" explicitly.
 */
@Configuration
public class Neo4jTransactionManagerConfig {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver, DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}
