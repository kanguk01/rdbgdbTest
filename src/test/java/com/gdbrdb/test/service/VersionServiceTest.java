//package com.gdbrdb.test.service;
//
//import com.gdbrdb.test.entity.mysql.VersionEntity;
//import com.gdbrdb.test.entity.neo4j.VersionNode;
//import com.gdbrdb.test.service.VersionService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.List;
//
//// import 등 생략
//
//@SpringBootTest
//public class VersionServiceTest {
//
//    @Autowired
//    private VersionService versionService;
//
//    @Test
//    void testCompareMySQLvsNeo4jQueries() {
//
//        /* ========================= */
//        /* 1. 샘플 데이터 생성(작게) */
//        /* ========================= */
//
//        // MySQL
//        VersionEntity v1 = versionService.createMySQLVersion("MySQL_v1", null);
//        VersionEntity v2 = versionService.createMySQLVersion("MySQL_v2", List.of(v1.getId()));
//        VersionEntity v3 = versionService.createMySQLVersion("MySQL_v3", List.of(v1.getId()));
//        VersionEntity v4 = versionService.createMySQLVersion("MySQL_v4", List.of(v2.getId(), v3.getId()));
//        // v4는 v2, v3를 부모로 하는 머지 예시
//
//        // Neo4j
//        VersionNode n1 = versionService.createNeo4jVersion("Neo4j_v1", null);
//        VersionNode n2 = versionService.createNeo4jVersion("Neo4j_v2", List.of(n1.getNodeId()));
//        VersionNode n3 = versionService.createNeo4jVersion("Neo4j_v3", List.of(n1.getId()));
//        VersionNode n4 = versionService.createNeo4jVersion("Neo4j_v4", List.of(n2.getId(), n3.getId()));
//
//        /* ========================= */
//        /* 2. Ancestor 쿼리 실행 시간 측정 */
//        /* ========================= */
//
//        long startMySQL = System.nanoTime();
//        var mysqlAncestorsV4 = versionService.getMySQLAllAncestorIds(v4.getId());
//        long endMySQL = System.nanoTime();
//        long mysqlDuration = endMySQL - startMySQL;
//
//        System.out.println("[MySQL] Ancestors of v4 = " + mysqlAncestorsV4);
//        System.out.println("[MySQL] Ancestor query took (ns) = " + mysqlDuration);
//
//        long startNeo4j = System.nanoTime();
//        var neo4jAncestorsN4 = versionService.getNeo4jAllAncestors(n4.getId());
//        long endNeo4j = System.nanoTime();
//        long neo4jDuration = endNeo4j - startNeo4j;
//
//        System.out.println("[Neo4j] Ancestors of n4 = " +
//                neo4jAncestorsN4.stream().map(VersionNode::getId).toList());
//        System.out.println("[Neo4j] Ancestor query took (ns) = " + neo4jDuration);
//
//        /* ========================= */
//        /* 3. LCA 쿼리 실행 시간 측정 */
//        /* ========================= */
//
//        // MySQL - LCA of (v2, v3)
//        startMySQL = System.nanoTime();
//        Long lcaMySQL = versionService.getMySQLLowestCommonAncestor(v2.getId(), v3.getId());
//        endMySQL = System.nanoTime();
//        System.out.println("[MySQL] LCA of (v2, v3) = " + lcaMySQL
//                + ", took (ns) = " + (endMySQL - startMySQL));
//
//        // Neo4j - LCA of (n2, n3)
//        startNeo4j = System.nanoTime();
//        VersionNode lcaNeo4j = versionService.getNeo4jLowestCommonAncestor(n2.getId(), n3.getId());
//        endNeo4j = System.nanoTime();
//        System.out.println("[Neo4j] LCA of (n2, n3) = " +
//                (lcaNeo4j != null ? lcaNeo4j.getId() : null) +
//                ", took (ns) = " + (endNeo4j - startNeo4j));
//
//        /* ========================= */
//        /* 결과 관찰:
//         * - 실행 시간이 크게 차이 안 날 수도 있고,
//         *   데이터가 많아질수록 (수천~수만) 차이가 생길 가능성.
//         * - 쿼리 복잡도는 Neo4j가 더 단순(Cypher)인 점 강조 가능.
//         */
//    }
//}