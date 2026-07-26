package com.traverse.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

/**
 * The Elasticsearch view of a travel. This is a denormalized, search-optimised
 * copy of the travel that lives in travel-service's Postgres (the source of
 * truth). Text fields are analyzed for full-text search; the id mirrors the
 * Postgres travel id so updates/deletes stay in sync.
 */
@Document(indexName = "travels")
public class TravelDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private List<String> destinationCities;

    @Field(type = FieldType.Text)
    private List<String> destinationCountries;

    @Field(type = FieldType.Text)
    private List<String> activities;

    @Field(type = FieldType.Text)
    private List<String> accommodations;

    @Field(type = FieldType.Text)
    private List<String> transportationTypes;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endDate;

    @Field(type = FieldType.Integer)
    private Integer durationDays;

    @Field(type = FieldType.Long)
    private Long managerId;

    public TravelDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getDestinationCities() {
        return destinationCities;
    }

    public void setDestinationCities(List<String> destinationCities) {
        this.destinationCities = destinationCities;
    }

    public List<String> getDestinationCountries() {
        return destinationCountries;
    }

    public void setDestinationCountries(List<String> destinationCountries) {
        this.destinationCountries = destinationCountries;
    }

    public List<String> getActivities() {
        return activities;
    }

    public void setActivities(List<String> activities) {
        this.activities = activities;
    }

    public List<String> getAccommodations() {
        return accommodations;
    }

    public void setAccommodations(List<String> accommodations) {
        this.accommodations = accommodations;
    }

    public List<String> getTransportationTypes() {
        return transportationTypes;
    }

    public void setTransportationTypes(List<String> transportationTypes) {
        this.transportationTypes = transportationTypes;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }
}
