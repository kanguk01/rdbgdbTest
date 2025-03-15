package com.gdbrdb.test.service;

import com.gdbrdb.test.service.VersionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 테스트 데이터(체인, 이진트리, 복합트리) 생성 테스트
 * MySQL / Neo4j 각각에 대해 대량 생성 후, 시간과 메모리 사용량을 로그로 확인
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataGenerationTest {

    @Autowired
    private VersionService versionService;

    /**
     * 체인 데이터 생성 (MySQL)
     */
    @Order(1)
    @Test
    @DisplayName("MySQL Chain Data Generation")
    void generateMySQLChainDataTest() {
        int scale = 10000; // 원하는 스케일로 조정
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateMySQLChainData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();

        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[MySQL-CHAIN] scale=" + scale
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 체인 데이터 생성 (Neo4j)
     */
    @Order(2)
    @Test
    @DisplayName("Neo4j Chain Data Generation")
    void generateNeo4jChainDataTest() {
        int scale = 10000; // 원하는 스케일로 조정
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateNeo4jChainData();  // <-- 이제 배치 삽입임

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();

        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[Neo4j-CHAIN] scale=" + scale
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 이진 트리 데이터 생성 (MySQL)
     */
    @Order(3)
    @Test
    @DisplayName("MySQL Binary Tree Data Generation")
    void generateMySQLBinaryTreeDataTest() {
        int scale = 10000;
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateMySQLBinaryTreeData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();

        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[MySQL-TREE] scale=" + scale
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 이진 트리 데이터 생성 (Neo4j)
     */
    @Order(4)
    @Test
    @DisplayName("Neo4j Binary Tree Data Generation")
    void generateNeo4jBinaryTreeDataTest() {
        int scale = 10000;
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateNeo4jBinaryTreeData();  // 배치 삽입

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();

        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[Neo4j-TREE] scale=" + scale
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 복합(불규칙) 트리 데이터 생성 (MySQL)
     */
    @Order(5)
    @Test
    @DisplayName("MySQL Complex Tree Data Generation")
    void generateMySQLComplexTreeDataTest() {
        int scale = 10000;
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateMySQLComplexTreeData();

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();

        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[MySQL-COMPLEX] scale=" + scale
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

    /**
     * 복합(불규칙) 트리 데이터 생성 (Neo4j)
     */
    @Order(6)
    @Test
    @DisplayName("Neo4j Complex Tree Data Generation")
    void generateNeo4jComplexTreeDataTest() {
        int scale = 10000;
        long startMemory = versionService.getUsedMemory();
        long startTime = System.currentTimeMillis();

        versionService.generateNeo4jComplexTreeData(); // 배치 삽입

        long endTime = System.currentTimeMillis();
        long endMemory = versionService.getUsedMemory();

        long timeElapsed = endTime - startTime;
        long memoryUsed = endMemory - startMemory;

        System.out.println("[Neo4j-COMPLEX] scale=" + scale
                + ", time=" + timeElapsed + "ms"
                + ", memUsed=" + memoryUsed + " bytes");
    }

}
