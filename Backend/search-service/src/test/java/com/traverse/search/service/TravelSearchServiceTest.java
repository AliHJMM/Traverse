package com.traverse.search.service;

import com.traverse.search.document.TravelDocument;
import com.traverse.search.dto.TravelIndexRequest;
import com.traverse.search.repository.TravelSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelSearchServiceTest {

    @Mock
    private TravelSearchRepository repository;
    @Mock
    private ElasticsearchOperations elasticsearchTemplate;
    @InjectMocks
    private TravelSearchService service;

    private TravelDocument doc(String title, List<String> cities) {
        TravelDocument d = new TravelDocument();
        d.setId("1");
        d.setTitle(title);
        d.setDestinationCities(cities);
        return d;
    }

    @SuppressWarnings("unchecked")
    private void stubSearch(TravelDocument... docs) {
        SearchHits<TravelDocument> hits = org.mockito.Mockito.mock(SearchHits.class);
        when(hits.stream()).thenReturn(Stream.of(docs).map(d -> {
            SearchHit<TravelDocument> hit = org.mockito.Mockito.mock(SearchHit.class);
            when(hit.getContent()).thenReturn(d);
            return hit;
        }));
        when(elasticsearchTemplate.search(any(CriteriaQuery.class), eq(TravelDocument.class))).thenReturn(hits);
    }

    @Test
    void indexBuildsAndSavesDocument() {
        TravelIndexRequest req = new TravelIndexRequest(7L, "Japan Tour",
                List.of("Tokyo", "Kyoto"), List.of("Japan"), List.of("Temple visit"),
                List.of("Hotel"), List.of("Flight"), LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10), 9, 42L);

        service.index(req);

        ArgumentCaptor<TravelDocument> captor = ArgumentCaptor.forClass(TravelDocument.class);
        verify(repository).save(captor.capture());
        TravelDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("7");
        assertThat(saved.getTitle()).isEqualTo("Japan Tour");
        assertThat(saved.getDestinationCities()).containsExactly("Tokyo", "Kyoto");
        assertThat(saved.getManagerId()).isEqualTo(42L);
    }

    @Test
    void deleteRemovesById() {
        service.delete(5L);
        verify(repository).deleteById("5");
    }

    @Test
    void searchBlankReturnsEmptyWithoutHittingElasticsearch() {
        assertThat(service.search("  ")).isEmpty();
        verifyNoInteractions(elasticsearchTemplate);
    }

    @Test
    void searchReturnsMatchingDocuments() {
        stubSearch(doc("Japan Tour", List.of("Tokyo")));
        List<TravelDocument> results = service.search("tokyo");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Japan Tour");
    }

    @Test
    void autocompleteBlankReturnsEmpty() {
        assertThat(service.autocomplete("")).isEmpty();
        verifyNoInteractions(elasticsearchTemplate);
    }

    @Test
    void autocompleteReturnsPrefixMatchedSuggestions() {
        stubSearch(doc("Tokyo Adventure", List.of("Tokyo", "Osaka")));
        List<String> suggestions = service.autocomplete("tok");
        // title "Tokyo Adventure" contains "tok"; city "Tokyo" starts with "tok"; "Osaka" doesn't
        assertThat(suggestions).contains("Tokyo Adventure", "Tokyo");
        assertThat(suggestions).doesNotContain("Osaka");
    }
}
