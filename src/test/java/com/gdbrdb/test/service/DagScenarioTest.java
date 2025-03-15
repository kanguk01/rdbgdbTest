package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.DagVersionEntity;
import com.gdbrdb.test.entity.neo4j.DagVersionNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DagScenarioTest {

    @Autowired
    private DagScenarioService scenarioService;

    /**
     * Scenario A - 조상 중 author='kanguk' AND title LIKE '%pdf%'
     * testId: 1~10000 범위 랜덤으로 선택
     */
    @Order(1)
    @Test
    @DisplayName("Scenario A - 조상 중 author='kanguk' AND title LIKE '%pdf%' (랜덤 노드)")
    void testScenarioA() {
        long testId = randomNodeId();  // 1..10000 중 랜덤
        String author = "kanguk";
        String titlePart = "pdf";

        // MySQL
        long msStart = System.currentTimeMillis();
        List<DagVersionEntity> mySqlRes =
                scenarioService.findMySqlAncestorsByAuthorTitle(testId, author, titlePart);
        long msEnd = System.currentTimeMillis();
        System.out.println("[MySQL] node=" + testId
                + ", Ancestors count=" + mySqlRes.size() + ", time=" + (msEnd - msStart) + "ms");

        // Neo4j
        long neoStart = System.currentTimeMillis();
        List<DagVersionNode> neoRes =
                scenarioService.findNeoAncestorsByAuthorTitle(String.valueOf(testId), author, titlePart);
        long neoEnd = System.currentTimeMillis();
        System.out.println("[Neo4j] node=" + testId
                + ", Ancestors count=" + neoRes.size() + ", time=" + (neoEnd - neoStart) + "ms");
    }

    /**
     * Scenario B - 자손 중 createdTime >= 2025-03-01
     * testId: 1~10000 범위 랜덤으로 선택
     */
    @Order(2)
    @Test
    @DisplayName("Scenario B - 자손 중 createdTime >= 2025-03-01 (랜덤 노드)")
    void testScenarioB() {
        long testId = randomNodeId();  // 1..10000 중 랜덤
        LocalDateTime threshold = LocalDateTime.of(2025, 3, 1, 0, 0);

        // MySQL
        long msStart = System.currentTimeMillis();
        List<DagVersionEntity> mySqlDesc =
                scenarioService.findMySqlDescendantsCreatedAfter(testId, threshold);
        long msEnd = System.currentTimeMillis();
        System.out.println("[MySQL] node=" + testId
                + ", Descendants after " + threshold
                + " => count=" + mySqlDesc.size() + ", time=" + (msEnd - msStart) + "ms");

        // Neo4j
        long neoStart = System.currentTimeMillis();
        List<DagVersionNode> neoDesc =
                scenarioService.findNeoDescendantsCreatedAfter(String.valueOf(testId), threshold);
        long neoEnd = System.currentTimeMillis();
        System.out.println("[Neo4j] node=" + testId
                + ", Descendants after " + threshold
                + " => count=" + neoDesc.size() + ", time=" + (neoEnd - neoStart) + "ms");
    }

    /**
     * Scenario C - 두 노드의 최신 공통 조상
     * 여기서는 idA, idB 둘 다 랜덤으로 선택
     */
    @Order(3)
    @Test
    @DisplayName("Scenario C - 두 노드의 최신 공통 조상 (랜덤 노드 A, B)")
    void testScenarioC() {
        long idA = randomNodeId();
        long idB = randomNodeId();
        // 같을 수도 있지만, 일단 랜덤 pick

        // MySQL
        long msStart = System.currentTimeMillis();
        DagVersionEntity mySqlLatest = scenarioService.findMySqlLatestCommonAncestor(idA, idB);
        long msEnd = System.currentTimeMillis();
        System.out.println("[MySQL] (A=" + idA + ", B=" + idB + ") LatestCommonAncestor => "
                + (mySqlLatest != null ? mySqlLatest.getId() : "null")
                + ", time=" + (msEnd - msStart) + "ms");

        // Neo4j
        long neoStart = System.currentTimeMillis();
        var neoLatest =
                scenarioService.findNeoLatestCommonAncestor(String.valueOf(idA), String.valueOf(idB));
        long neoEnd = System.currentTimeMillis();
        System.out.println("[Neo4j] (A=" + idA + ", B=" + idB + ") LatestCommonAncestor => "
                + (neoLatest != null ? neoLatest.getNodeId() : "null")
                + ", time=" + (neoEnd - neoStart) + "ms");
    }

    /**
     * Scenario D - 랜덤 노드 N번 반복 (A/B/C 시나리오)
     */
    @Order(4)
    @Test
    @DisplayName("Scenario D - 여러 랜덤 노드로 A/B/C 시나리오 반복")
    void testScenarioD() {
        // 예: 5회 반복
        for (int i = 0; i < 5; i++) {
            long randomId = randomNodeId();
            // A
            testScenarioA_one(randomId);
            // B
            testScenarioB_one(randomId);
            // C
            long idA = randomNodeId();
            long idB = randomNodeId();
            testScenarioC_one(idA, idB);
            System.out.println("----");
        }
    }

    /**
     * 시나리오 A 하나 돌리는 메서드
     */
    private void testScenarioA_one(long testId) {
        String author = "kanguk";
        String titlePart = "pdf";
        List<DagVersionEntity> ms =
                scenarioService.findMySqlAncestorsByAuthorTitle(testId, author, titlePart);
        List<DagVersionNode> neo =
                scenarioService.findNeoAncestorsByAuthorTitle(String.valueOf(testId), author, titlePart);
        System.out.println("[ScenarioA] node=" + testId
                + " => MySQL=" + ms.size() + ", Neo4j=" + neo.size());
    }

    /**
     * 시나리오 B 하나 돌리는 메서드
     */
    private void testScenarioB_one(long testId) {
        LocalDateTime t = LocalDateTime.of(2025, 3, 1, 0, 0);
        List<DagVersionEntity> ms =
                scenarioService.findMySqlDescendantsCreatedAfter(testId, t);
        List<DagVersionNode> neo =
                scenarioService.findNeoDescendantsCreatedAfter(String.valueOf(testId), t);
        System.out.println("[ScenarioB] node=" + testId
                + " => MySQL=" + ms.size() + ", Neo4j=" + neo.size());
    }

    /**
     * 시나리오 C 하나 돌리는 메서드
     */
    private void testScenarioC_one(long idA, long idB) {
        DagVersionEntity ms =
                scenarioService.findMySqlLatestCommonAncestor(idA, idB);
        var neo =
                scenarioService.findNeoLatestCommonAncestor(String.valueOf(idA), String.valueOf(idB));
        System.out.println("[ScenarioC] (A=" + idA + ",B=" + idB + ") => MySQL="
                + (ms != null ? ms.getId() : "null")
                + ", Neo4j="
                + (neo != null ? neo.getNodeId() : "null"));
    }

    /**
     * 1..10000 범위의 랜덤 ID
     */
    private long randomNodeId() {
        return ThreadLocalRandom.current().nextLong(1, 10001);
    }
}
