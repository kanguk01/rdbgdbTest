package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.DagVersionEntity;
import com.gdbrdb.test.entity.neo4j.DagVersionNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 여러 시나리오(A/B/C/E)에 대해 10번씩 랜덤 노드를 뽑아 반복 실행,
 * 결과를 표 형태로 출력하는 예시.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DagScenarioTest {

    @Autowired
    private DagScenarioService scenarioService;

    // 반복 횟수
    private static final int REPEAT_COUNT = 10;

    // 테스트 결과를 담을 리스트
    // (시나리오 구분, iteration, testId(또는 idA/B), MySQL/Neo4j 시간, 결과개수 등)
    private static final List<ResultRecord> results = new ArrayList<>();

    // ======================================
    // 1) 시나리오 A 10회 반복
    // ======================================
    @Test
    @Order(1)
    @DisplayName("Scenario A - 10회 반복")
    void testScenarioARepeat() {
        String scenarioName = "A";
        for (int i = 1; i <= REPEAT_COUNT; i++) {
            long testId = randomNodeId();
            String author = "kanguk";
            String titlePart = "pdf";

            // MySQL
            long msStart = System.currentTimeMillis();
            List<DagVersionEntity> mySqlRes =
                    scenarioService.findMySqlAncestorsByAuthorTitle(testId, author, titlePart);
            long msEnd = System.currentTimeMillis();

            long mysqlTime = msEnd - msStart;
            int mysqlCount = mySqlRes.size();

            // Neo4j
            long neoStart = System.currentTimeMillis();
            List<DagVersionNode> neoRes =
                    scenarioService.findNeoAncestorsByAuthorTitle(String.valueOf(testId), author, titlePart);
            long neoEnd = System.currentTimeMillis();

            long neoTime = neoEnd - neoStart;
            int neoCount = neoRes.size();

            // 결과 저장
            results.add(new ResultRecord(
                    scenarioName, i,
                    "testId=" + testId,
                    mysqlTime, mysqlCount,
                    neoTime, neoCount
            ));
        }
    }

    // ======================================
    // 2) 시나리오 B 10회 반복
    // ======================================
    @Test
    @Order(2)
    @DisplayName("Scenario B - 10회 반복")
    void testScenarioBRepeat() {
        String scenarioName = "B";
        LocalDateTime threshold = LocalDateTime.of(2025, 3, 1, 0, 0);

        for (int i = 1; i <= REPEAT_COUNT; i++) {
            long testId = randomNodeId();

            // MySQL
            long msStart = System.currentTimeMillis();
            List<DagVersionEntity> mySqlDesc =
                    scenarioService.findMySqlDescendantsCreatedAfter(testId, threshold);
            long msEnd = System.currentTimeMillis();

            long mysqlTime = msEnd - msStart;
            int mysqlCount = mySqlDesc.size();

            // Neo4j
            long neoStart = System.currentTimeMillis();
            List<DagVersionNode> neoDesc =
                    scenarioService.findNeoDescendantsCreatedAfter(String.valueOf(testId), threshold);
            long neoEnd = System.currentTimeMillis();

            long neoTime = neoEnd - neoStart;
            int neoCount = neoDesc.size();

            // 결과 저장
            results.add(new ResultRecord(
                    scenarioName, i,
                    "testId=" + testId,
                    mysqlTime, mysqlCount,
                    neoTime, neoCount
            ));
        }
    }

    // ======================================
    // 3) 시나리오 C 10회 반복
    // ======================================
    @Test
    @Order(3)
    @DisplayName("Scenario C - 10회 반복")
    void testScenarioCRepeat() {
        String scenarioName = "C";

        for (int i = 1; i <= REPEAT_COUNT; i++) {
            long idA = randomNodeId();
            long idB = randomNodeId();

            // MySQL
            long msStart = System.currentTimeMillis();
            DagVersionEntity mySqlLatest =
                    scenarioService.findMySqlLatestCommonAncestor(idA, idB);
            long msEnd = System.currentTimeMillis();

            long mysqlTime = msEnd - msStart;
            int mysqlCount = (mySqlLatest == null) ? 0 : 1;

            // Neo4j
            long neoStart = System.currentTimeMillis();
            DagVersionNode neoLatest =
                    scenarioService.findNeoLatestCommonAncestor(String.valueOf(idA), String.valueOf(idB));
            long neoEnd = System.currentTimeMillis();

            long neoTime = neoEnd - neoStart;
            int neoCount = (neoLatest == null) ? 0 : 1;

            // 결과 저장
            results.add(new ResultRecord(
                    scenarioName, i,
                    "(A=" + idA + ", B=" + idB + ")",
                    mysqlTime, mysqlCount,
                    neoTime, neoCount
            ));
        }
    }

    // ======================================
    // 4) 시나리오 E 10회 반복
    // ======================================
    @Test
    @Order(4)
    @DisplayName("Scenario E - 10회 반복 (3단계 패턴)")
    void testScenarioERepeat() {
        String scenarioName = "E";
        String author = "chulsu";
        String titlePart = "ppt";

        for (int i = 1; i <= REPEAT_COUNT; i++) {
            long testId = randomNodeId();

            // MySQL
            long msStart = System.currentTimeMillis();
            List<DagVersionEntity> msRes =
                    scenarioService.findMySqlUpTo3StepsByAuthorTitle(testId, author, titlePart);
            long msEnd = System.currentTimeMillis();

            long mysqlTime = msEnd - msStart;
            int mysqlCount = msRes.size();

            // Neo4j
            long neoStart = System.currentTimeMillis();
            List<DagVersionNode> neoRes =
                    scenarioService.findNeoUpTo3StepsByAuthorTitle(String.valueOf(testId), author, titlePart);
            long neoEnd = System.currentTimeMillis();

            long neoTime = neoEnd - neoStart;
            int neoCount = neoRes.size();

            // 결과 저장
            results.add(new ResultRecord(
                    scenarioName, i,
                    "testId=" + testId,
                    mysqlTime, mysqlCount,
                    neoTime, neoCount
            ));
        }
    }

    // ======================================
    // 모든 테스트가 끝난 뒤, 결과를 표로 출력
    // ======================================
    @AfterAll
    static void printResultsAsTable() {
        System.out.println("\n============================================");
        System.out.println(" Test Results (Scenario A/B/C/E, 10 times)  ");
        System.out.println("============================================");
        // 테이블 헤더
        System.out.printf("%-8s | %-5s | %-18s | %-12s | %-10s | %-12s | %-10s\n",
                "Scenario", "Iter", "NodeInfo", "MySQLTime(ms)", "MySQLCount",
                "Neo4jTime(ms)", "Neo4jCount");
        System.out.println("--------------------------------------------------------------------------------------");

        for (ResultRecord r : results) {
            System.out.printf("%-8s | %5d | %-18s | %12d | %10d | %12d | %10d\n",
                    r.scenario, r.iteration, r.nodeDesc,
                    r.mysqlTime, r.mysqlCount,
                    r.neoTime, r.neoCount);
        }
        System.out.println("--------------------------------------------------------------------------------------\n");
    }

    /**
     * 랜덤 노드 ID (1..10000)
     */
    private long randomNodeId() {
        return ThreadLocalRandom.current().nextLong(1, 10001);
    }

    /**
     * 결과를 담기 위한 작은 DTO
     */
    static class ResultRecord {
        String scenario; // A/B/C/E
        int iteration;   // 반복 회차
        String nodeDesc; // "testId=xxx" or "(A=...,B=...)"

        long mysqlTime;  // ms
        int mysqlCount;
        long neoTime;    // ms
        int neoCount;

        public ResultRecord(String scenario, int iteration, String nodeDesc,
                            long mysqlTime, int mysqlCount,
                            long neoTime, int neoCount) {
            this.scenario = scenario;
            this.iteration = iteration;
            this.nodeDesc = nodeDesc;
            this.mysqlTime = mysqlTime;
            this.mysqlCount = mysqlCount;
            this.neoTime = neoTime;
            this.neoCount = neoCount;
        }
    }
}
