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

    // --- Part 2: travel feature graph (for Neo4j recommendations) ---------
    // A Travel node is linked to its feature nodes (destinations, activities,
    // accommodation types, transport types). Travelers get PARTICIPATED_IN /
    // RATED edges (written by the subscription + feedback flows), and
    // recommendations are computed by feature overlap with what a traveler
    // has participated in, weighted by the feedback they gave.

    /** Ensure the Travel node exists and clear its old feature edges (for updates). */
    @Query("""
            MERGE (t:Travel {travelId: $travelId})
            WITH t
            OPTIONAL MATCH (t)-[r:HAS_DESTINATION|HAS_ACTIVITY|HAS_ACCOMMODATION|HAS_TRANSPORT]->()
            DELETE r
            """)
    void resetTravelFeatures(@Param("travelId") Long travelId);

    @Query("""
            MATCH (t:Travel {travelId: $travelId})
            MERGE (d:Destination {city: $city})
            ON CREATE SET d.country = $country
            MERGE (t)-[:HAS_DESTINATION]->(d)
            """)
    void linkDestination(@Param("travelId") Long travelId, @Param("city") String city,
                         @Param("country") String country);

    @Query("""
            MATCH (t:Travel {travelId: $travelId})
            MERGE (a:Activity {name: $name})
            MERGE (t)-[:HAS_ACTIVITY]->(a)
            """)
    void linkActivity(@Param("travelId") Long travelId, @Param("name") String name);

    @Query("""
            MATCH (t:Travel {travelId: $travelId})
            MERGE (a:AccommodationType {type: $type})
            MERGE (t)-[:HAS_ACCOMMODATION]->(a)
            """)
    void linkAccommodation(@Param("travelId") Long travelId, @Param("type") String type);

    @Query("""
            MATCH (t:Travel {travelId: $travelId})
            MERGE (tr:TransportType {type: $type})
            MERGE (t)-[:HAS_TRANSPORT]->(tr)
            """)
    void linkTransport(@Param("travelId") Long travelId, @Param("type") String type);

    @Query("MATCH (t:Travel {travelId: $travelId}) DETACH DELETE t")
    void deleteTravelNode(@Param("travelId") Long travelId);

    /**
     * Records that a traveler participated in a travel (called when a
     * subscription is confirmed). MERGE keeps it idempotent.
     */
    @Query("""
            MERGE (u:Traveler {userId: $userId})
            WITH u
            MATCH (t:Travel {travelId: $travelId})
            MERGE (u)-[:PARTICIPATED_IN]->(t)
            """)
    void recordParticipation(@Param("userId") Long userId, @Param("travelId") Long travelId);

    /** Removes a participation edge (called when a traveler unsubscribes). */
    @Query("""
            MATCH (u:Traveler {userId: $userId})-[r:PARTICIPATED_IN]->(t:Travel {travelId: $travelId})
            DELETE r
            """)
    void removeParticipation(@Param("userId") Long userId, @Param("travelId") Long travelId);

    /** Records/updates a traveler's feedback rating on a participated travel. */
    @Query("""
            MERGE (u:Traveler {userId: $userId})
            WITH u
            MATCH (t:Travel {travelId: $travelId})
            MERGE (u)-[r:RATED]->(t)
            SET r.score = $score
            """)
    void recordRating(@Param("userId") Long userId, @Param("travelId") Long travelId,
                      @Param("score") int score);

    /**
     * Personalized recommendations: travels sharing features (destination /
     * activity / accommodation / transport -- 4 fields, well over the
     * "at least 3" requirement) with the travels this traveler participated
     * in, weighted by the feedback score they gave (default 3 when
     * unrated), excluding travels they've already joined. Returns travel ids
     * ranked best-first.
     */
    @Query("""
            MATCH (me:Traveler {userId: $userId})-[:PARTICIPATED_IN]->(mine:Travel)
                  -[r:HAS_DESTINATION|HAS_ACTIVITY|HAS_ACCOMMODATION|HAS_TRANSPORT]->(feature)
            OPTIONAL MATCH (me)-[f:RATED]->(mine)
            WITH me, feature, coalesce(f.score, 3) AS weight
            MATCH (rec:Travel)-[:HAS_DESTINATION|HAS_ACTIVITY|HAS_ACCOMMODATION|HAS_TRANSPORT]->(feature)
            WHERE NOT EXISTS { (me)-[:PARTICIPATED_IN]->(rec) }
            WITH rec, sum(weight) AS score, count(DISTINCT feature) AS sharedFeatures
            RETURN rec.travelId AS travelId
            ORDER BY score DESC, sharedFeatures DESC
            LIMIT 12
            """)
    List<Long> recommendTravelIds(@Param("userId") Long userId);
}
