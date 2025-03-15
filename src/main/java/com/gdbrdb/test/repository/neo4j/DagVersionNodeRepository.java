package com.gdbrdb.test.repository.neo4j;

import com.gdbrdb.test.entity.neo4j.DagVersionNode;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DagVersionNodeRepository extends org.springframework.data.neo4j.repository.Neo4jRepository<DagVersionNode, String> {

    @Query("""
        UNWIND $batch AS row
        CREATE (v:DagVersion {
          nodeId: row.nodeId,
          title: row.title,
          content: row.content,
          author: row.author,
          createdTime: row.createdTime
        })
        FOREACH (pId IN row.parents |
           MERGE (p:DagVersion { nodeId: pId })
           MERGE (p)-[:PARENT_OF]->(v)
        )
        RETURN count(v)
    """)
    Integer bulkInsertNodes(@Param("batch") List<Map<String, Object>> batch);

    /**
     * 시나리오 A) 특정 노드의 조상 중
     * author=... 이고 title CONTAINS ...
     */
    @Query("""
        MATCH (start:DagVersion { nodeId: $startId })<-[:PARENT_OF*]-(ancestor:DagVersion)
        WHERE ancestor.author = $author
          AND ancestor.title CONTAINS $titlePart
        RETURN ancestor
    """)
    List<DagVersionNode> findAncestorsByAuthorAndTitle(
            @Param("startId") String startId,
            @Param("author") String author,
            @Param("titlePart") String titlePart
    );

    /**
     * 시나리오 B) 특정 노드의 자손 중
     * createdTime >= $timeThreshold
     */
    @Query("""
        MATCH (start:DagVersion { nodeId: $startId })-[:PARENT_OF*]->(desc:DagVersion)
        WHERE desc.createdTime >= $timeThreshold
        RETURN desc
    """)
    List<DagVersionNode> findDescendantsCreatedAfter(
            @Param("startId") String startId,
            @Param("timeThreshold") LocalDateTime timeThreshold
    );

    /**
     * 시나리오 C) 두 노드(A,B)의 공통 조상 중
     * createdTime이 가장 늦은 노드 1개
     *
     * Cypher는 "공통 조상"을 구하기 위해
     * A<-[:PARENT_OF*]-X and B<-[:PARENT_OF*]-X 형태로 교집합을 찾을 수 있음
     */
    @Query("""
        MATCH (a:DagVersion {nodeId: $idA})<-[:PARENT_OF*]-(x:DagVersion),
              (b:DagVersion {nodeId: $idB})<-[:PARENT_OF*]-(x:DagVersion)
        RETURN x
        ORDER BY x.createdTime DESC
        LIMIT 1
    """)
    DagVersionNode findLatestCommonAncestor(
            @Param("idA") String idA,
            @Param("idB") String idB
    );
}
