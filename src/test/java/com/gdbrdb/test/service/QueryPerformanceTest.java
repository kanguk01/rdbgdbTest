//package com.gdbrdb.test.service;
//
//import com.gdbrdb.test.entity.neo4j.VersionNodeNew;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//@SpringBootTest
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class QueryPerformanceTest {
//
//    private static final Logger log = LoggerFactory.getLogger(QueryPerformanceTest.class);
//
//    private final VersionService versionService;
//
//    public QueryPerformanceTest(VersionService versionService) {
//        this.versionService = versionService;
//    }
//
//    /**
//     * 체인 데이터 성능 측정
//     * - 데이터는 "mysql_root_chain" / "neo4j_root_chain" 접두어로 생성된 데이터를 대상으로 함
//     */
//    @Test
//    @Order(1)
//    void testChainQueries() {
//        log.info("========== [Chain Data Query Test] ==========");
//        // 데이터 조회: 서비스에 구현된 getLast...Id() 메서드로 마지막 체인 노드와 중간 노드 조회
//        Long mysqlLastId = versionService.getLastMySQLChainId("mysql_root_chain");
//        String neo4jLastId = versionService.getLastNeo4jChainNodeId("neo4j_root_chain");
//        if (mysqlLastId == null || neo4jLastId == null) {
//            log.error("Chain data not found. Please run DataGenerationTest first.");
//            return;
//        }
//        log.info(">>> MySQL EXPLAIN Plan (Chain):");
//        List<Object[]> mysqlExplain = versionService.logAndGetMySQLExplainPlan(mysqlLastId);
//        mysqlExplain.forEach(row -> log.info("{}", Arrays.toString(row)));
//        log.info(">>> Neo4j EXPLAIN Plan (Chain):");
//        String neo4jExplain = versionService.logAndGetNeo4jExplainPlan(neo4jLastId);
//        log.info("{}", neo4jExplain);
//
//        // Ancestor 성능 측정 (20회)
//        List<Long> mysqlAncestorTimes = measureExecutionTimes(() -> versionService.getMySQLAllAncestorIds(mysqlLastId), 20);
//        List<Long> neo4jAncestorTimes = measureExecutionTimes(() -> versionService.getNeo4jAllAncestors(neo4jLastId), 20);
//        int mysqlAncestorCount = versionService.getMySQLAllAncestorIds(mysqlLastId).size();
//        int neo4jAncestorCount = versionService.getNeo4jAllAncestors(neo4jLastId).size();
//        log.info("[MySQL-Chain] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                mysqlAncestorCount, average(mysqlAncestorTimes), Collections.min(mysqlAncestorTimes), Collections.max(mysqlAncestorTimes));
//        log.info("[Neo4j-Chain] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                neo4jAncestorCount, average(neo4jAncestorTimes), Collections.min(neo4jAncestorTimes), Collections.max(neo4jAncestorTimes));
//
//        // LCA 성능 측정 (체인: 중간 vs 마지막)
//        Long mysqlMidId = versionService.getMySQLChainMidId("mysql_root_chain");
//        String neo4jMidId = versionService.getNeo4jChainMidNodeId("neo4j_root_chain");
//        List<Long> mysqlLCATimes = measureExecutionTimes(() -> versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId), 10);
//        List<Long> neo4jLCATimes = measureExecutionTimes(() -> {
//            VersionNodeNew node = versionService.getNeo4jLowestCommonAncestor(neo4jMidId, neo4jLastId);
//            return node != null ? 1L : 1L;
//        }, 10);
//        Long lcaMySQL = versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId);
//        VersionNodeNew lcaNeo4j = versionService.getNeo4jLowestCommonAncestor(neo4jMidId, neo4jLastId);
//        log.info("[MySQL-Chain] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                lcaMySQL, average(mysqlLCATimes), Collections.min(mysqlLCATimes), Collections.max(mysqlLCATimes));
//        log.info("[Neo4j-Chain] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                lcaNeo4j != null ? lcaNeo4j.getNodeId() : "null", average(neo4jLCATimes), Collections.min(neo4jLCATimes), Collections.max(neo4jLCATimes));
//    }
//
//    /**
//     * 이진 트리 데이터 성능 측정
//     * - 데이터는 "mysql_root_tree" / "neo4j_root_tree" 접두어로 생성된 데이터를 대상으로 함
//     */
//    @Test
//    @Order(2)
//    void testTreeQueries() {
//        log.info("========== [Binary Tree Data Query Test] ==========");
//        Long mysqlLastId = versionService.getLastMySQLBinaryTreeId("mysql_root_tree");
//        String neo4jLastId = versionService.getLastNeo4jBinaryTreeNodeId("neo4j_root_tree");
//        if (mysqlLastId == null || neo4jLastId == null) {
//            log.error("Binary tree data not found. Please run DataGenerationTest first.");
//            return;
//        }
//        log.info(">>> MySQL EXPLAIN Plan (Binary Tree):");
//        List<Object[]> mysqlExplain = versionService.logAndGetMySQLExplainPlan(mysqlLastId);
//        mysqlExplain.forEach(row -> log.info("{}", Arrays.toString(row)));
//        log.info(">>> Neo4j EXPLAIN Plan (Binary Tree):");
//        String neo4jExplain = versionService.logAndGetNeo4jExplainPlan(neo4jLastId);
//        log.info("{}", neo4jExplain);
//
//        List<Long> mysqlAncestorTimes = measureExecutionTimes(() -> versionService.getMySQLAllAncestorIds(mysqlLastId), 20);
//        List<Long> neo4jAncestorTimes = measureExecutionTimes(() -> versionService.getNeo4jAllAncestors(neo4jLastId), 20);
//        int mysqlAncestorCount = versionService.getMySQLAllAncestorIds(mysqlLastId).size();
//        int neo4jAncestorCount = versionService.getNeo4jAllAncestors(neo4jLastId).size();
//        log.info("[MySQL-Tree] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                mysqlAncestorCount, average(mysqlAncestorTimes), Collections.min(mysqlAncestorTimes), Collections.max(mysqlAncestorTimes));
//        log.info("[Neo4j-Tree] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                neo4jAncestorCount, average(neo4jAncestorTimes), Collections.min(neo4jAncestorTimes), Collections.max(neo4jAncestorTimes));
//
//        Long mysqlMidId = versionService.getMySQLTreeMidId("mysql_root_tree");
//        String neo4jMidId = versionService.getNeo4jTreeMidNodeId("neo4j_root_tree");
//        List<Long> mysqlLCATimes = measureExecutionTimes(() -> versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId), 10);
//        List<Long> neo4jLCATimes = measureExecutionTimes(() -> {
//            VersionNodeNew node = versionService.getNeo4jLowestCommonAncestor(neo4jMidId, neo4jLastId);
//            return node != null ? 1L : 1L;
//        }, 10);
//        Long lcaMySQL = versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId);
//        VersionNodeNew lcaNeo4j = versionService.getNeo4jLowestCommonAncestor(neo4jMidId, neo4jLastId);
//        log.info("[MySQL-Tree] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                lcaMySQL, average(mysqlLCATimes), Collections.min(mysqlLCATimes), Collections.max(mysqlLCATimes));
//        log.info("[Neo4j-Tree] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
//                lcaNeo4j != null ? lcaNeo4j.getNodeId() : "null", average(neo4jLCATimes), Collections.min(neo4jLCATimes), Collections.max(neo4jLCATimes));
//    }
//
//    /* ===== 측정 헬퍼 메서드 ===== */
//    private <T> List<Long> measureExecutionTimes(QueryExecutor<T> executor, int iterations) {
//        List<Long> times = new ArrayList<>();
//        // 워밍업 1회
//        executor.execute();
//        for (int i = 0; i < iterations; i++) {
//            long start = System.currentTimeMillis();
//            executor.execute();
//            long end = System.currentTimeMillis();
//            times.add(end - start);
//        }
//        return times;
//    }
//
//    private double average(List<Long> times) {
//        return times.stream().mapToLong(Long::longValue).average().orElse(0);
//    }
//
//    @FunctionalInterface
//    interface QueryExecutor<T> {
//        T execute();
//    }
//}