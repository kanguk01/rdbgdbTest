package com.gdbrdb.test.repository.neo4j;

import com.gdbrdb.test.entity.neo4j.VersionNodeNew;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface VersionNodeNewRepository extends Neo4jRepository<VersionNodeNew, String> {

    // 1) 배치 삽입
    @Query("""
           UNWIND $batch AS row
           CREATE (v:VersionNew {nodeId: row.nodeId, content: row.content})
           FOREACH (parentId IN row.parents |
             MERGE (p:VersionNew {nodeId: parentId})
             MERGE (p)-[:PARENT_OF]->(v)
           )
           RETURN count(v)
           """)
    Integer bulkInsertNodes(@Param("batch") List<Map<String, Object>> batch);

    // 2) "조상"은 child←parent 경로
    @Query("""
        MATCH (v:VersionNew {nodeId: $startId})<-[:PARENT_OF*]-(ancestor:VersionNew)
        RETURN ancestor
        """)
    List<VersionNodeNew> findAllAncestors(@Param("startId") String startId);

    // 3) "자손"은 parent→child 경로
    @Query("""
        MATCH (v:VersionNew {nodeId: $startId})-[:PARENT_OF*]->(descendant:VersionNew)
        RETURN descendant
        """)
    List<VersionNodeNew> findAllDescendants(@Param("startId") String startId);

    // 4) EXPLAIN 예시(조상)
    @Query("""
        EXPLAIN
        MATCH (v:VersionNew {nodeId: $startId})<-[:PARENT_OF*]-(ancestor:VersionNew)
        RETURN ancestor
        """)
    String explainAncestors(@Param("startId") String startId);
}
