package com.traverse.travel.graph;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Destination")
public class DestinationNode {

    @Id
    @GeneratedValue
    private Long id;

    private String city;

    private String country;

    public DestinationNode() {
    }

    public DestinationNode(String city, String country) {
        this.city = city;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
