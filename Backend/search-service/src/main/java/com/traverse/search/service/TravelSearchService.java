package com.traverse.search.service;

import com.traverse.search.document.TravelDocument;
import com.traverse.search.dto.TravelIndexRequest;
import com.traverse.search.repository.TravelSearchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TravelSearchService {

    private final TravelSearchRepository repository;
    private final ElasticsearchOperations elasticsearchTemplate;

    public TravelSearchService(TravelSearchRepository repository, ElasticsearchOperations elasticsearchTemplate) {
        this.repository = repository;
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    /** Upsert a travel into the index (create or update -- same id overwrites). */
    public void index(TravelIndexRequest r) {
        TravelDocument doc = new TravelDocument();
        doc.setId(String.valueOf(r.id()));
        doc.setTitle(r.title());
        doc.setDestinationCities(r.destinationCities());
        doc.setDestinationCountries(r.destinationCountries());
        doc.setActivities(r.activities());
        doc.setAccommodations(r.accommodations());
        doc.setTransportationTypes(r.transportationTypes());
        doc.setStartDate(r.startDate());
        doc.setEndDate(r.endDate());
        doc.setDurationDays(r.durationDays());
        doc.setManagerId(r.managerId());
        repository.save(doc);
    }

    public void delete(Long id) {
        repository.deleteById(String.valueOf(id));
    }

    /**
     * Full-text search across the travel's most meaningful fields (title,
     * destinations, countries, activities) -- an analyzed `match` on each,
     * OR'd together, so "tokyo", "japan", or "temple" all find the trip.
     */
    public List<TravelDocument> search(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        Criteria criteria = new Criteria("title").matches(q)
                .or(new Criteria("destinationCities").matches(q))
                .or(new Criteria("destinationCountries").matches(q))
                .or(new Criteria("activities").matches(q));
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(0, 50));
        SearchHits<TravelDocument> hits = elasticsearchTemplate.search(query, TravelDocument.class);
        return hits.stream().map(SearchHit::getContent).toList();
    }

    /**
     * Autocomplete: prefix-match the query against titles and destination
     * names, returning distinct human-readable suggestions. Input is
     * lowercased to line up with the analyzer's lowercased terms.
     */
    public List<String> autocomplete(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        String prefix = q.toLowerCase();
        Criteria criteria = new Criteria("title").startsWith(prefix)
                .or(new Criteria("destinationCities").startsWith(prefix))
                .or(new Criteria("destinationCountries").startsWith(prefix));
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(0, 10));
        SearchHits<TravelDocument> hits = elasticsearchTemplate.search(query, TravelDocument.class);

        Set<String> suggestions = new LinkedHashSet<>();
        hits.stream().map(SearchHit::getContent).forEach(d -> {
            if (d.getTitle() != null && d.getTitle().toLowerCase().contains(prefix)) {
                suggestions.add(d.getTitle());
            }
            addMatching(suggestions, d.getDestinationCities(), prefix);
            addMatching(suggestions, d.getDestinationCountries(), prefix);
        });
        return suggestions.stream().limit(10).toList();
    }

    private void addMatching(Set<String> out, List<String> values, String prefix) {
        if (values == null) {
            return;
        }
        for (String v : values) {
            if (v != null && v.toLowerCase().startsWith(prefix)) {
                out.add(v);
            }
        }
    }
}
