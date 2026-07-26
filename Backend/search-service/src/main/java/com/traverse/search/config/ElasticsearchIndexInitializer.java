package com.traverse.search.config;

import com.traverse.search.document.TravelDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Creates the "travels" index (with the mapping derived from TravelDocument)
 * on startup if it doesn't already exist. Wrapped in try/catch so a briefly
 * unavailable Elasticsearch doesn't crash the service on boot -- it'll be
 * created lazily on the first index/search once ES is reachable.
 */
@Component
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexInitializer.class);

    private final ElasticsearchOperations operations;

    public ElasticsearchIndexInitializer(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexOperations indexOps = operations.indexOps(TravelDocument.class);
            if (!indexOps.exists()) {
                indexOps.createWithMapping();
                log.info("Created Elasticsearch index 'travels' with mapping");
            }
        } catch (Exception e) {
            log.warn("Could not initialize 'travels' index at startup (Elasticsearch may still be warming up): {}",
                    e.getMessage());
        }
    }
}
