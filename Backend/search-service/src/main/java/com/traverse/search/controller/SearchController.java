package com.traverse.search.controller;

import com.traverse.search.document.TravelDocument;
import com.traverse.search.dto.AutocompleteResponse;
import com.traverse.search.dto.TravelIndexRequest;
import com.traverse.search.service.TravelSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final TravelSearchService searchService;

    public SearchController(TravelSearchService searchService) {
        this.searchService = searchService;
    }

    /** Traveler-facing full-text travel search. */
    @GetMapping("/travels")
    public List<TravelDocument> search(@RequestParam("q") String query) {
        return searchService.search(query);
    }

    /** Traveler-facing autocomplete suggestions. */
    @GetMapping("/autocomplete")
    public AutocompleteResponse autocomplete(@RequestParam("q") String query) {
        return new AutocompleteResponse(searchService.autocomplete(query));
    }

    /** Internal: travel-service pushes here to keep the index in sync. */
    @PostMapping("/index")
    public ResponseEntity<Void> index(@Valid @RequestBody TravelIndexRequest request) {
        searchService.index(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /** Internal: travel-service calls this when a travel is deleted. */
    @DeleteMapping("/index/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        searchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
