package com.traverse.travel.graph;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DestinationNodeRepository extends Neo4jRepository<DestinationNode, Long> {

    Optional<DestinationNode> findByCity(String city);

    /**
     * Every time an itinerary visits city A right before city B, this bumps
     * a CONNECTED_TO relationship's trip count between them -- the graph is
     * a byproduct of real itineraries, not seed data, so "nearby" gets more
     * meaningful as more travels get created.
     */
    @Query("""
            MERGE (a:Destination {city: $cityA})
            ON CREATE SET a.country = $countryA
            MERGE (b:Destination {city: $cityB})
            ON CREATE SET b.country = $countryB
            MERGE (a)-[r:CONNECTED_TO]->(b)
            ON CREATE SET r.tripCount = 1
            ON MATCH SET r.tripCount = r.tripCount + 1
            """)
    void connect(@Param("cityA") String cityA, @Param("countryA") String countryA,
                 @Param("cityB") String cityB, @Param("countryB") String countryB);

    @Query("""
            MATCH (a:Destination {city: $city})-[:CONNECTED_TO*1..2]-(b:Destination)
            WHERE a <> b
            RETURN DISTINCT b
            """)
    List<DestinationNode> findNearby(@Param("city") String city);
}
