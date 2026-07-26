package com.traverse.search.repository;

import com.traverse.search.document.TravelDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TravelSearchRepository extends ElasticsearchRepository<TravelDocument, String> {
}
