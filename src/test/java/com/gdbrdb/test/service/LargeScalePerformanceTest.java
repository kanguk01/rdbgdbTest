package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.VersionEntity;
import com.gdbrdb.test.entity.neo4j.VersionNodeNew;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LargeScalePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(LargeScalePerformanceTest.class);

    @Autowired
    private VersionService versionService;

    // 데이터 생성 테스트 (최초 한 번 실행 후, 이후 측정 테스트만 실행)
    @Test
    @Order(1)
    void generateData() {
        int chainScale = 1000;
        int treeScale = 1000;
        int complexScale = 1000;
        log.info("========== [Data Generation] ==========");
        versionService.generateMySQLChainData(chainScale);
        versionService.generateNeo4jChainData(chainScale);
        versionService.generateMySQLBinaryTreeData(treeScale);
        versionService.generateNeo4jBinaryTreeData(treeScale);
        versionService.generateMySQLComplexTreeData(complexScale);
        versionService.generateNeo4jComplexTreeData(complexScale);
        log.info("Data generation complete.");
    }

    // 체인 데이터 측정 테스트 (DB에서 조회)
    @Test
    @Order(2)
    void testChainQueries() {
        log.info("========== [Chain Data Query Test] ==========");
        Long mysqlLastId = versionService.getLastMySQLChainId();
        String neo4jLastId = versionService.getLastNeo4jChainNodeId();
        if (mysqlLastId == null || neo4jLastId == null) {
            log.error("Chain data not found. Generate data first.");
            return;
        }
        log.info(">>> MySQL EXPLAIN Plan (Chain):");
        List<Object[]> mysqlExplain = versionService.logAndGetMySQLExplainPlan(mysqlLastId);
        mysqlExplain.forEach(row -> log.info("{}", Arrays.toString(row)));
        log.info(">>> Neo4j EXPLAIN Plan (Chain):");
        String neo4jExplain = versionService.logAndGetNeo4jExplainPlan(neo4jLastId);
        log.info("{}", neo4jExplain);

        List<Long> mysqlAncestorTimes = measureExecutionTimes(() -> versionService.getMySQLAllAncestorIds(mysqlLastId), 20);
        List<Long> neo4jAncestorTimes = measureExecutionTimes(() -> versionService.getNeo4jAllAncestors(neo4jLastId), 20);
        int mysqlAncestorCount = versionService.getMySQLAllAncestorIds(mysqlLastId).size();
        int neo4jAncestorCount = versionService.getNeo4jAllAncestors(neo4jLastId).size();
        log.info("[MySQL-Chain] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                mysqlAncestorCount, average(mysqlAncestorTimes), Collections.min(mysqlAncestorTimes), Collections.max(mysqlAncestorTimes));
        log.info("[Neo4j-Chain] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                neo4jAncestorCount, average(neo4jAncestorTimes), Collections.min(neo4jAncestorTimes), Collections.max(neo4jAncestorTimes));

        Long mysqlMidId = versionService.getMySQLChainMidId();
        VersionNodeNew neo4jMidNode = versionService.getNeo4jChainMidNode();
        List<Long> mysqlLCATimes = measureExecutionTimes(() -> versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId), 10);
        List<Long> neo4jLCATimes = measureExecutionTimes(() -> {
            VersionNodeNew node = versionService.getNeo4jLowestCommonAncestor(neo4jMidNode.getNodeId(), neo4jLastId);
            return node != null ? 1L : 1L;
        }, 10);
        Long lcaMySQL = versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId);
        VersionNodeNew lcaNeo4j = versionService.getNeo4jLowestCommonAncestor(neo4jMidNode.getNodeId(), neo4jLastId);
        log.info("[MySQL-Chain] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                lcaMySQL, average(mysqlLCATimes), Collections.min(mysqlLCATimes), Collections.max(mysqlLCATimes));
        log.info("[Neo4j-Chain] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                lcaNeo4j != null ? lcaNeo4j.getNodeId() : "null", average(neo4jLCATimes), Collections.min(neo4jLCATimes), Collections.max(neo4jLCATimes));
    }

    // 이진 트리 데이터 측정 테스트 (DB에서 조회)
    @Test
    @Order(3)
    void testTreeQueries() {
        log.info("========== [Binary Tree Data Query Test] ==========");
        Long mysqlLastId = versionService.getLastMySQLBinaryTreeId();
        String neo4jLastId = versionService.getLastNeo4jBinaryTreeNodeId();
        if (mysqlLastId == null || neo4jLastId == null) {
            log.error("Binary tree data not found. Generate data first.");
            return;
        }
        log.info(">>> MySQL EXPLAIN Plan (Binary Tree):");
        List<Object[]> mysqlExplain = versionService.logAndGetMySQLExplainPlan(mysqlLastId);
        mysqlExplain.forEach(row -> log.info("{}", Arrays.toString(row)));
        log.info(">>> Neo4j EXPLAIN Plan (Binary Tree):");
        String neo4jExplain = versionService.logAndGetNeo4jExplainPlan(neo4jLastId);
        log.info("{}", neo4jExplain);

        List<Long> mysqlAncestorTimes = measureExecutionTimes(() -> versionService.getMySQLAllAncestorIds(mysqlLastId), 20);
        List<Long> neo4jAncestorTimes = measureExecutionTimes(() -> versionService.getNeo4jAllAncestors(neo4jLastId), 20);
        int mysqlAncestorCount = versionService.getMySQLAllAncestorIds(mysqlLastId).size();
        int neo4jAncestorCount = versionService.getNeo4jAllAncestors(neo4jLastId).size();
        log.info("[MySQL-Tree] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                mysqlAncestorCount, average(mysqlAncestorTimes), Collections.min(mysqlAncestorTimes), Collections.max(mysqlAncestorTimes));
        log.info("[Neo4j-Tree] Ancestor count: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                neo4jAncestorCount, average(neo4jAncestorTimes), Collections.min(neo4jAncestorTimes), Collections.max(neo4jAncestorTimes));

        Long mysqlMidId = versionService.getMySQLTreeMidId();
        VersionNodeNew neo4jMidNode = versionService.getNeo4jTreeMidNode();
        List<Long> mysqlLCATimes = measureExecutionTimes(() -> versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId), 10);
        List<Long> neo4jLCATimes = measureExecutionTimes(() -> {
            VersionNodeNew node = versionService.getNeo4jLowestCommonAncestor(neo4jMidNode.getNodeId(), neo4jLastId);
            return node != null ? 1L : 1L;
        }, 10);
        Long lcaMySQL = versionService.getMySQLLowestCommonAncestor(mysqlMidId, mysqlLastId);
        VersionNodeNew lcaNeo4j = versionService.getNeo4jLowestCommonAncestor(neo4jMidNode.getNodeId(), neo4jLastId);
        log.info("[MySQL-Tree] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                lcaMySQL, average(mysqlLCATimes), Collections.min(mysqlLCATimes), Collections.max(mysqlLCATimes));
        log.info("[Neo4j-Tree] LCA: {}, Avg Time = {} ms, Min = {} ms, Max = {} ms",
                lcaNeo4j != null ? lcaNeo4j.getNodeId() : "null", average(neo4jLCATimes), Collections.min(neo4jLCATimes), Collections.max(neo4jLCATimes));
    }

    /* 측정 헬퍼 메서드 */
    private <T> List<Long> measureExecutionTimes(QueryExecutor<T> executor, int iterations) {
        List<Long> times = new ArrayList<>();
        // 워밍업 1회
        executor.execute();
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            executor.execute();
            long end = System.currentTimeMillis();
            times.add(end - start);
        }
        return times;
    }

    private double average(List<Long> times) {
        return times.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    @FunctionalInterface
    interface QueryExecutor<T> {
        T execute();
    }
}
