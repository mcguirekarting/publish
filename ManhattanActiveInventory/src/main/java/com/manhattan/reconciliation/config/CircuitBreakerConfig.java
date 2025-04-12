package com.manhattan.reconciliation.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for circuit breaker patterns used in the application.
 */
@Configuration
public class CircuitBreakerConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfig.class);
    
    /**
     * Creates a registry event consumer to log circuit breaker events.
     *
     * @return the registry event consumer
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                entryAddedEvent.getAddedEntry().getEventPublisher()
                        .onFailureRateExceeded(event -> logger.warn("Circuit breaker {} failure rate exceeded: {}",
                                event.getCircuitBreakerName(), event.getFailureRate()))
                        .onStateTransition(event -> logger.info("Circuit breaker {} state changed from {} to {}",
                                event.getCircuitBreakerName(), event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                        .onSlowCallRateExceeded(event -> logger.warn("Circuit breaker {} slow call rate exceeded: {}",
                                event.getCircuitBreakerName(), event.getSlowCallRate()));
            }
            
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                logger.info("Circuit breaker {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }
            
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                logger.info("Circuit breaker {} replaced", entryReplacedEvent.getOldEntry().getName());
            }
        };
    }
    
    /**
     * Creates a circuit breaker registry with custom configurations.
     *
     * @return the circuit breaker registry
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
}