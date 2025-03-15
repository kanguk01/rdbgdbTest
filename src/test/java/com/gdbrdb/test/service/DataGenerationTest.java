package com.gdbrdb.test.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 테스트 데이터(체인, 이진트리, 복합트리) 생성 테스트
 * - MySQL / Neo4j 각각에 대해 대량 생성 후, 시간과 메모리 사용량을 로그로 확인
 * - 체인(1..scale), 이진(scale+1..2*scale)은 기존 방식
 * - 복합(2*scale+1..3*scale)은 VersionService.generateComplexTreeData()로
 *   MySQL & Neo4j 동시에 "동일한" 랜덤 구조를 생성
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataGenerationTest {

    @Autowired
    private VersionService versionService;

    private static final int SCALE = 1000;  // 스케일

    /**
     * 1) MySQL Chain
     */
    @Order(1)
    @Test
    @DisplayName("MySQL Chain Data Generation")
    void generateMySQLChainDataTest() {
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateMySQLChainData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();
        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[MySQL-CHAIN] scale=" + SCALE
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 2) Neo4j Chain
     */
    @Order(2)
    @Test
    @DisplayName("Neo4j Chain Data Generation")
    void generateNeo4jChainDataTest() {
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateNeo4jChainData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();
        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[Neo4j-CHAIN] scale=" + SCALE
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 3) MySQL Binary
     */
    @Order(3)
    @Test
    @DisplayName("MySQL Binary Tree Data Generation")
    void generateMySQLBinaryTreeDataTest() {
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateMySQLBinaryTreeData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();
        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[MySQL-TREE] scale=" + SCALE
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 4) Neo4j Binary
     */
    @Order(4)
    @Test
    @DisplayName("Neo4j Binary Tree Data Generation")
    void generateNeo4jBinaryTreeDataTest() {
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateNeo4jBinaryTreeData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();
        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[Neo4j-TREE] scale=" + SCALE
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 5) 복합(불규칙) 트리 데이터 (MySQL + Neo4j 동시에)
     *    => VersionService.generateComplexTreeData() 한 번 호출로
     *       MySQL, Neo4j 모두 같은 랜덤 구조를 삽입
     */
    @Order(5)
    @Test
    @DisplayName("Complex Tree Data Generation (MySQL + Neo4j together)")
    void generateComplexTreeDataTest() {
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        // 한 번만 호출 => 내부에서 MySQL -> Neo4j 순으로 삽입
        versionService.generateComplexTreeData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();
        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[Complex-Tree] scale=" + SCALE
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

}
