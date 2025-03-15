//package com.gdbrdb.test.repository.neo4j;
//
//import com.gdbrdb.test.entity.neo4j.VersionNode;
//import org.springframework.data.neo4j.repository.Neo4jRepository;
//import org.springframework.data.neo4j.repository.query.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//
//public interface VersionNodeRepository extends Neo4jRepository<VersionNode, String> {
//
//    /**
//     * 특정 노드의 모든 조상(ancestor) 조회
//     */
//    @Query("""
//        MATCH (v:Version {nodeId: $startId})-[:PARENT_OF*]->(ancestor:Version)
//        RETURN ancestor
//        """)
//    List<VersionNode> findAllAncestors(@Param("startId") String startId);
//
//    /**
//     * 특정 노드의 모든 자식(children) 조회 (역방향)
//     */
//    @Query("""
//        MATCH (v:Version {nodeId: $startId})<-[:PARENT_OF*]-(descendant:Version)
//        RETURN descendant
//        """)
//    List<VersionNode> findAllDescendants(@Param("startId") String startId);
//
//    /**
//     * EXPLAIN 실행 계획: 특정 노드의 조상 조회 쿼리에 대해 실행 계획을 확인.
//     */
//    @Query("""
//        EXPLAIN
//        MATCH (v:Version {nodeId: $startId})-[:PARENT_OF*]->(ancestor:Version)
//        RETURN ancestor
//        """)
//    String explainAncestors(@Param("startId") String startId);
//}