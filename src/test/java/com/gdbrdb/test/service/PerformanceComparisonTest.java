package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.VersionEntity;
import com.gdbrdb.test.entity.neo4j.VersionNodeNew;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Random;

/**
 * 이미 생성된 데이터를 가지고 쿼리 성능, LCA 계산, 조상조회 등
 * 시간을 비교/측정하는 테스트.
 *
 * 가정:
 * - 1~1000       : Chain 데이터
 * - 1001~2000    : Binary Tree
 * - 2001~3000    : Complex Tree
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceComparisonTest {

    @Autowired
    private VersionService versionService;

    // 구간별 ID 범위 상수
    private static final int CHAIN_START = 1;
    private static final int CHAIN_END = 1000;

    private static final int BINARY_START = 1001;
    private static final int BINARY_END = 2000;

    private static final int COMPLEX_START = 2001;
    private static final int COMPLEX_END = 3000;

    /*
       ------------------------------------------------------
         1) 체인 구간 (1~1000)에 대한 성능 테스트
       ------------------------------------------------------
     */

    @Order(1)
    @Test
    @DisplayName("Chain - Compare getAllAncestors (MySQL vs Neo4j)")
    void compareAllAncestorsChainTest() {
        // 임의로 중간 노드 하나 고르기 (ex: 500)
        int testId = (CHAIN_START + CHAIN_END) / 2; // 500
        compareAllAncestorsTestCommon(testId);
    }

    @Order(2)
    @Test
    @DisplayName("Chain - Compare LCA (MySQL vs Neo4j)")
    void compareLcaChainTest() {
        // 예: 300, 700 (체인 내에서만)
        int idA = 300;
        int idB = 700;
        compareLcaTestCommon(idA, idB);
    }

    @Order(3)
    @Test
    @DisplayName("Chain - Compare Descendants (MySQL vs Neo4j)")
    void compareDescendantsChainTest() {
        // 체인의 루트는 1번
        compareDescendantsTestCommon(CHAIN_START);
    }

    @Order(4)
    @Test
    @DisplayName("Chain - Random multiple queries (MySQL vs Neo4j)")
    void randomMultipleQueriesChainTest() {
        randomMultipleQueriesTestCommon(CHAIN_START, CHAIN_END, 10);
    }

    /*
       ------------------------------------------------------
         2) 이진 트리 구간 (1001~2000)에 대한 성능 테스트
       ------------------------------------------------------
     */

    @Order(5)
    @Test
    @DisplayName("Binary - Compare getAllAncestors (MySQL vs Neo4j)")
    void compareAllAncestorsBinaryTest() {
        // 예: 범위 중간 = 1500 정도
        int testId = (BINARY_START + BINARY_END) / 2; // 1500
        compareAllAncestorsTestCommon(testId);
    }

    @Order(6)
    @Test
    @DisplayName("Binary - Compare LCA (MySQL vs Neo4j)")
    void compareLcaBinaryTest() {
        // 예: 1200, 1400
        int idA = 1200;
        int idB = 1400;
        compareLcaTestCommon(idA, idB);
    }

    @Order(7)
    @Test
    @DisplayName("Binary - Compare Descendants (MySQL vs Neo4j)")
    void compareDescendantsBinaryTest() {
        // 이진 트리의 루트는 1001
        compareDescendantsTestCommon(BINARY_START);
    }

    @Order(8)
    @Test
    @DisplayName("Binary - Random multiple queries (MySQL vs Neo4j)")
    void randomMultipleQueriesBinaryTest() {
        randomMultipleQueriesTestCommon(BINARY_START, BINARY_END, 10);
    }

    /*
       ------------------------------------------------------
         3) 복합(불규칙) 트리 구간 (2001~3000)에 대한 성능 테스트
       ------------------------------------------------------
     */

    @Order(9)
    @Test
    @DisplayName("Complex - Compare getAllAncestors (MySQL vs Neo4j)")
    void compareAllAncestorsComplexTest() {
        // 예: 범위 중간 = 2500
        int testId = (COMPLEX_START + COMPLEX_END) / 2; // 2500
        compareAllAncestorsTestCommon(testId);
    }

    @Order(10)
    @Test
    @DisplayName("Complex - Compare LCA (MySQL vs Neo4j)")
    void compareLcaComplexTest() {
        // 예: 2100, 2700
        int idA = 2100;
        int idB = 2700;
        compareLcaTestCommon(idA, idB);
    }

    @Order(11)
    @Test
    @DisplayName("Complex - Compare Descendants (MySQL vs Neo4j)")
    void compareDescendantsComplexTest() {
        // 복합 트리의 루트는 2001
        compareDescendantsTestCommon(COMPLEX_START);
    }

    @Order(12)
    @Test
    @DisplayName("Complex - Random multiple queries (MySQL vs Neo4j)")
    void randomMultipleQueriesComplexTest() {
        randomMultipleQueriesTestCommon(COMPLEX_START, COMPLEX_END, 10);
    }

    /*
       ------------------------------------------------------
         아래부터는 "공통 로직" 메서드들
       ------------------------------------------------------
     */

    /**
     * [공통] 특정 MySQL(Long) ID & Neo4j(String) ID에 대해서
     * getAllAncestors 조회 시간을 비교.
     */
    private void compareAllAncestorsTestCommon(int testId) {
        // MySQL
        long mysqlStartMem = versionService.getUsedMemory();
        long mysqlStart = System.currentTimeMillis();

        List<Long> mysqlAncestors = versionService.getMySQLAllAncestorIds((long) testId);

        long mysqlEnd = System.currentTimeMillis();
        long mysqlEndMem = versionService.getUsedMemory();

        long mysqlTime = mysqlEnd - mysqlStart;
        long mysqlMem = mysqlEndMem - mysqlStartMem;
        System.out.println("[MySQL getAllAncestors] node=" + testId
                + ", time=" + mysqlTime + "ms, usedMem=" + mysqlMem
                + ", ancestorsCount=" + mysqlAncestors.size());

        // Neo4j
        long neoStartMem = versionService.getUsedMemory();
        long neoStart = System.currentTimeMillis();

        List<VersionNodeNew> neoAncestors = versionService.getNeo4jAllAncestors(String.valueOf(testId));

        long neoEnd = System.currentTimeMillis();
        long neoEndMem = versionService.getUsedMemory();

        long neoTime = neoEnd - neoStart;
        long neoMem = neoEndMem - neoStartMem;
        System.out.println("[Neo4j getAllAncestors] node=" + testId
                + ", time=" + neoTime + "ms, usedMem=" + neoMem
                + ", ancestorsCount=" + neoAncestors.size());
    }

    /**
     * [공통] MySQL vs Neo4j LCA (Lowest Common Ancestor) 비교
     */
    private void compareLcaTestCommon(int idA, int idB) {
        // MySQL
        long mysqlStartMem = versionService.getUsedMemory();
        long mysqlStart = System.currentTimeMillis();

        Long mysqlLca = versionService.getMySQLLowestCommonAncestor((long) idA, (long) idB);

        long mysqlEnd = System.currentTimeMillis();
        long mysqlEndMem = versionService.getUsedMemory();

        long mysqlTime = mysqlEnd - mysqlStart;
        long mysqlMem = mysqlEndMem - mysqlStartMem;
        System.out.println("[MySQL LCA] (" + idA + ", " + idB + ") => " + mysqlLca
                + ", time=" + mysqlTime + "ms, memUsed=" + mysqlMem);

        // Neo4j
        long neoStartMem = versionService.getUsedMemory();
        long neoStart = System.currentTimeMillis();

        VersionNodeNew neoLca = versionService.getNeo4jLowestCommonAncestor(String.valueOf(idA), String.valueOf(idB));

        long neoEnd = System.currentTimeMillis();
        long neoEndMem = versionService.getUsedMemory();

        long neoTime = neoEnd - neoStart;
        long neoMem = neoEndMem - neoStartMem;

        String neoLcaId = (neoLca != null) ? neoLca.getNodeId() : "null";
        System.out.println("[Neo4j LCA] (" + idA + ", " + idB + ") => " + neoLcaId
                + ", time=" + neoTime + "ms, memUsed=" + neoMem);
    }

    /**
     * [공통] MySQL vs Neo4j - 특정 노드의 "자손(Descendants)" 조회
     * - MySQL: 재귀 CTE로 descendants 구함
     * - Neo4j: repo.findAllDescendants(...) 구함
     */
    private void compareDescendantsTestCommon(int startId) {
        // MySQL
        long mysqlStartMem = versionService.getUsedMemory();
        long mysqlStart = System.currentTimeMillis();

        // MySQL descendants
        List<Long> mysqlDesc = versionService.getMysqlRepo().findAllDescendantIds((long) startId);

        long mysqlEnd = System.currentTimeMillis();
        long mysqlEndMem = versionService.getUsedMemory();

        long mysqlTime = mysqlEnd - mysqlStart;
        long mysqlMem = mysqlEndMem - mysqlStartMem;
        System.out.println("[MySQL descendants] startNode=" + startId
                + ", count=" + mysqlDesc.size()
                + ", time=" + mysqlTime + "ms, memUsed=" + mysqlMem);

        // Neo4j
        long neoStartMem = versionService.getUsedMemory();
        long neoStart = System.currentTimeMillis();

        List<VersionNodeNew> neoDesc = versionService.getNeo4jNewRepo().findAllDescendants(String.valueOf(startId));

        long neoEnd = System.currentTimeMillis();
        long neoEndMem = versionService.getUsedMemory();

        long neoTime = neoEnd - neoStart;
        long neoMem = neoEndMem - neoStartMem;
        System.out.println("[Neo4j descendants] startNode=" + startId
                + ", count=" + neoDesc.size()
                + ", time=" + neoTime + "ms, memUsed=" + neoMem);
    }

    /**
     * [공통] 임의 노드 ID를 여러 번 뽑아서 MySQL / Neo4j 조상 조회
     *  - 범위: [startId..endId]
     */
    private void randomMultipleQueriesTestCommon(int startId, int endId, int queries) {
        Random rand = new Random();

        long totalMySqlTime = 0L;
        long totalNeoTime = 0L;

        for (int i = 0; i < queries; i++) {
            int randomId = startId + rand.nextInt(endId - startId + 1);
            // randomId in [startId..endId]

            // MySQL
            long startMy = System.nanoTime();
            List<Long> mySqlAnc = versionService.getMySQLAllAncestorIds((long) randomId);
            long endMy = System.nanoTime();

            long diffMy = endMy - startMy;
            totalMySqlTime += diffMy;

            // Neo4j
            long startNeo = System.nanoTime();
            List<VersionNodeNew> neoAnc = versionService.getNeo4jAllAncestors(String.valueOf(randomId));
            long endNeo = System.nanoTime();

            long diffNeo = endNeo - startNeo;
            totalNeoTime += diffNeo;

            System.out.println("#" + (i+1)
                    + " queryId=" + randomId
                    + " | MySQLAncSize=" + mySqlAnc.size() + ", time(ns)=" + diffMy
                    + " | Neo4jAncSize=" + neoAnc.size() + ", time(ns)=" + diffNeo);
        }

        System.out.println("[Summary] MySQL average time(ns) = " + (totalMySqlTime / queries));
        System.out.println("[Summary] Neo4j average time(ns) = " + (totalNeoTime / queries));
    }

}
