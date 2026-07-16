package com.traverse.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "destination_city")
    private String destinationCity;

    private LocalDate date;

    private BigDecimal cost;

    protected Activity() {
    }

    public Activity(String name, String description, String destinationCity, LocalDate date, BigDecimal cost) {
        this.name = name;
        this.description = description;
        this.destinationCity = destinationCity;
        this.date = date;
        this.cost = cost;
    }

    public Long getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
    }

    public void setTravel(Travel travel) {
        this.travel = travel;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getCost() {
        return cost;
    }
}
