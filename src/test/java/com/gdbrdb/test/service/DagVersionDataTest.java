package com.gdbrdb.test.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * "다중 부모(DAG)" 형태의 데이터 생성 테스트
 * DagVersionService.generateDagData()를 통해
 * - MySQL (dag_version, dag_version_parents)
 * - Neo4j (DagVersion) 노드
 * 총 1만개를 생성
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DagVersionDataTest {

    @Autowired
    private DagVersionService dagService;

    @Test
    @DisplayName("Generate DAG data (MySQL + Neo4j) - 10,000 nodes")
    @Order(1)
    void testGenerateDagData() {
        dagService.generateDagData();
    }
}
