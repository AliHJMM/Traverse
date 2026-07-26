package com.traverse.search.controller;

import com.traverse.search.document.TravelDocument;
import com.traverse.search.service.TravelSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerTest {

    private TravelSearchService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TravelSearchService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(service)).build();
    }

    @Test
    void searchReturnsResults() throws Exception {
        TravelDocument d = new TravelDocument();
        d.setId("1");
        d.setTitle("Japan Tour");
        when(service.search("japan")).thenReturn(List.of(d));

        mockMvc.perform(get("/api/search/travels").param("q", "japan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Japan Tour"));
    }

    @Test
    void autocompleteReturnsSuggestions() throws Exception {
        when(service.autocomplete("tok")).thenReturn(List.of("Tokyo", "Tokyo Adventure"));

        mockMvc.perform(get("/api/search/autocomplete").param("q", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]").value("Tokyo"));
    }

    @Test
    void indexReturnsNoContent() throws Exception {
        String body = """
                {"id":9,"title":"Alps Trip","destinationCities":["Zurich"],"durationDays":6}
                """;
        mockMvc.perform(post("/api/search/index").contentType("application/json").content(body))
                .andExpect(status().isNoContent());
        verify(service).index(any());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/search/index/9"))
                .andExpect(status().isNoContent());
        verify(service).delete(eq(9L));
    }
}
