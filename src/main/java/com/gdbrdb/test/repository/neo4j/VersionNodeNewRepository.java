package com.gdbrdb.test.repository.neo4j;

import com.gdbrdb.test.entity.neo4j.VersionNodeNew;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;

public interface VersionNodeNewRepository extends Neo4jRepository<VersionNodeNew, String> {

    /**
     * UNWIND를 이용한 배치 삽입 쿼리
     * 각 row는 { nodeId, content, parents } 구조의 Map이어야 합니다.
     * 'parents'는 부모 노드의 nodeId(String) 리스트입니다.
     */
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

    /**
     * 특정 노드의 모든 조상 조회
     */
    @Query("""
        MATCH (v:VersionNew {nodeId: $startId})-[:PARENT_OF*]->(ancestor:VersionNew)
        RETURN ancestor
        """)
    List<VersionNodeNew> findAllAncestors(@Param("startId") String startId);

    /**
     * 특정 노드의 모든 자식 조회 (역방향)
     */
    @Query("""
        MATCH (v:VersionNew {nodeId: $startId})<-[:PARENT_OF*]-(descendant:VersionNew)
        RETURN descendant
        """)
    List<VersionNodeNew> findAllDescendants(@Param("startId") String startId);

    /**
     * EXPLAIN 실행 계획: 특정 노드의 조상 조회 쿼리에 대해 실행 계획을 확인.
     */
    @Query("""
        EXPLAIN 
        MATCH (v:VersionNew {nodeId: $startId})-[:PARENT_OF*]->(ancestor:VersionNew)
        RETURN ancestor
        """)
    String explainAncestors(@Param("startId") String startId);
}
