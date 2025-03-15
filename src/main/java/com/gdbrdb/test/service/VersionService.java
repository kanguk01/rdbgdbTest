package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.VersionEntity;
import com.gdbrdb.test.entity.neo4j.VersionNodeBatchDTO;
import com.gdbrdb.test.entity.neo4j.VersionNodeNew;
import com.gdbrdb.test.repository.mysql.VersionRepository;
import com.gdbrdb.test.repository.neo4j.VersionNodeNewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * VersionService
 * - scale에 따라 (체인, 이진, 복합) 각각 [1..scale], [scale+1..2*scale], [2*scale+1..3*scale]로 생성
 */
@Service
@RequiredArgsConstructor
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    /** 원하는 규모(scale) - 여기서 한 번만 조정하면 됨 */
    private static final int SCALE = 10000;

    // 체인 범위
    private static final int CHAIN_START = 1;
    private static final int CHAIN_END   = SCALE;

    // 이진 트리 범위
    private static final int BINARY_START = SCALE + 1;
    private static final int BINARY_END   = 2 * SCALE;

    // 복합 트리 범위
    private static final int COMPLEX_START = 2 * SCALE + 1;
    private static final int COMPLEX_END   = 3 * SCALE;

    // MySQL 관련 (VersionEntity)
    private final VersionRepository mysqlRepo;

    // Neo4j 관련 (VersionNodeNew)
    private final VersionNodeNewRepository neo4jNewRepo;

    /* ======================= */
    /*       MySQL PART        */
    /* ======================= */

    public VersionRepository getMysqlRepo() {
        return this.mysqlRepo;
    }

    /**
     * MySQL 버전 단건 생성 (다중 부모 지원)
     */
    public VersionEntity createMySQLVersion(String content, List<Long> parentIds) {
        VersionEntity newVersion = new VersionEntity();
        newVersion.setContent(content);
        if (parentIds != null && !parentIds.isEmpty()) {
            List<VersionEntity> parents = mysqlRepo.findAllById(parentIds);
            for (VersionEntity p : parents) {
                newVersion.addParent(p);
            }
        }
        return mysqlRepo.save(newVersion);
    }

    /**
     * MySQL: 특정 버전의 모든 조상 ID 조회 (재귀 CTE)
     */
    public List<Long> getMySQLAllAncestorIds(Long versionId) {
        return mysqlRepo.findAllAncestorIds(versionId);
    }

    /**
     * MySQL: 두 버전의 최하단 공통 조상(LCA)
     */
    public Long getMySQLLowestCommonAncestor(Long idA, Long idB) {
        Set<Long> ancestorsA = new HashSet<>(getMySQLAllAncestorIds(idA));
        ancestorsA.add(idA);
        Set<Long> ancestorsB = new HashSet<>(getMySQLAllAncestorIds(idB));
        ancestorsB.add(idB);
        ancestorsA.retainAll(ancestorsB);
        if (ancestorsA.isEmpty()) return null;
        return ancestorsA.stream().max(Long::compareTo).orElse(null);
    }

    /**
     * MySQL: EXPLAIN 실행 계획
     */
    public List<Object[]> logAndGetMySQLExplainPlan(Long id) {
        List<Object[]> plan = mysqlRepo.explainSelectById(id);
        log.info("[MySQL EXPLAIN] Plan for id {}: {}", id, plan);
        return plan;
    }

    /* ======================= */
    /*  Neo4j (VersionNew) PART  */
    /* ======================= */

    public VersionNodeNewRepository getNeo4jNewRepo() {
        return this.neo4jNewRepo;
    }

    /**
     * Neo4j (VersionNew) 단건 생성
     * - 대량 시엔 bulkInsertNeo4jNodes 사용 권장
     */
    public VersionNodeNew createNeo4jVersion(String nodeId, String content, List<String> parentNodeIds) {
        VersionNodeNew newNode = new VersionNodeNew(nodeId, content);
        if (parentNodeIds != null && !parentNodeIds.isEmpty()) {
            List<VersionNodeNew> parents = neo4jNewRepo.findAllById(parentNodeIds);
            parents.forEach(newNode::addParent);
        }
        return neo4jNewRepo.save(newNode);
    }

    /**
     * nodeId 없이 생성할 때 (비추)
     */
    public VersionNodeNew createNeo4jVersion(String content, List<String> parentNodeIds) {
        return createNeo4jVersion(UUID.randomUUID().toString(), content, parentNodeIds);
    }

    /**
     * Neo4j: 특정 노드의 모든 조상 조회
     */
    public List<VersionNodeNew> getNeo4jAllAncestors(String nodeId) {
        return neo4jNewRepo.findAllAncestors(nodeId);
    }

    /**
     * Neo4j: 두 노드의 최하단 공통 조상(LCA)
     */
    public VersionNodeNew getNeo4jLowestCommonAncestor(String idA, String idB) {
        List<VersionNodeNew> ancestorsA = new ArrayList<>(getNeo4jAllAncestors(idA));
        VersionNodeNew nodeA = neo4jNewRepo.findById(idA).orElse(null);
        if (nodeA != null) ancestorsA.add(nodeA);

        List<VersionNodeNew> ancestorsB = new ArrayList<>(getNeo4jAllAncestors(idB));
        VersionNodeNew nodeB = neo4jNewRepo.findById(idB).orElse(null);
        if (nodeB != null) ancestorsB.add(nodeB);

        Set<String> aIds = ancestorsA.stream().map(VersionNodeNew::getNodeId).collect(Collectors.toSet());
        Set<String> bIds = ancestorsB.stream().map(VersionNodeNew::getNodeId).collect(Collectors.toSet());
        aIds.retainAll(bIds);
        if (aIds.isEmpty()) return null;

        // "가장 큰" 문자열 ID
        String candidateId = aIds.stream().max(String::compareTo).orElse(null);
        return candidateId != null ? neo4jNewRepo.findById(candidateId).orElse(null) : null;
    }

    /**
     * Neo4j: EXPLAIN
     */
    public String logAndGetNeo4jExplainPlan(String nodeId) {
        String plan = neo4jNewRepo.explainAncestors(nodeId);
        log.info("[Neo4j EXPLAIN] Plan for nodeId {}: {}", nodeId, plan);
        return plan;
    }

    /**
     * 메모리 사용량 측정
     */
    public long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /* =============================== */
    /*       Batch Insert (Neo4j)      */
    /* =============================== */

    public Integer bulkInsertNeo4jNodes(List<VersionNodeBatchDTO> dtos) {
        List<Map<String, Object>> batch = dtos.stream().map(dto -> {
            Map<String, Object> map = new HashMap<>();
            map.put("nodeId", dto.getNodeId());
            map.put("content", dto.getContent());
            map.put("parents", dto.getParentNodeIds());
            return map;
        }).collect(Collectors.toList());

        Integer count = neo4jNewRepo.bulkInsertNodes(batch);
        log.info("[Neo4j Bulk Insert] Inserted {} nodes", count);
        return count;
    }

    /* ================================================================= */
    /*                       MySQL 데이터 생성 메서드                     */
    /* ================================================================= */

    // ------------------- MySQL 체인: [1..scale] -------------------
    public void generateMySQLChainData() {
        // 루트 (ID=1) => 부모 없음
        createMySQLVersion("mysql_root_chain_" + CHAIN_START, null);

        for (long i = CHAIN_START + 1; i <= CHAIN_END; i++) {
            createMySQLVersion("mysql_v_chain_" + i, Collections.singletonList(i - 1));
        }
        log.info("[MySQL-Chain] Generated chain data (range {}..{}). Count={}",
                CHAIN_START, CHAIN_END, (CHAIN_END - CHAIN_START + 1));
    }

    // ------------------- MySQL 이진 트리: [scale+1..2*scale] -------------------
    public void generateMySQLBinaryTreeData() {
        // 루트 (ID=BINARY_START)
        createMySQLVersion("mysql_root_tree_" + BINARY_START, null);

        for (long i = BINARY_START + 1; i <= BINARY_END; i++) {
            // 오프셋 계산: i - BINARY_START => [1..(BINARY_END - BINARY_START)]
            long offset = i - BINARY_START;
            // parentOffset = offset/2 => 부모의 offset
            long parentOffset = offset / 2;
            // 실제 부모ID = BINARY_START + parentOffset
            long parentId = BINARY_START + parentOffset;

            createMySQLVersion("mysql_node_tree_" + i, Collections.singletonList(parentId));
        }
        log.info("[MySQL-Tree] Generated binary tree data (range {}..{}). Count={}",
                BINARY_START, BINARY_END, (BINARY_END - BINARY_START + 1));
    }

    // ------------------- MySQL 복합 트리: [2*scale+1..3*scale] -------------------
    public void generateMySQLComplexTreeData() {
        // 루트 (ID=COMPLEX_START)
        createMySQLVersion("mysql_root_complex_" + COMPLEX_START, null);
        Random rand = new Random();

        for (long i = COMPLEX_START + 1; i <= COMPLEX_END; i++) {
            // 현재 노드 i -> 부모는 [COMPLEX_START..(i-1)] 중 일부
            long rangeSize = (i - 1) - COMPLEX_START + 1; // i-1 - complexStart + 1
            if (rangeSize < 1) rangeSize = 1;
            long parent = COMPLEX_START + rand.nextInt((int)rangeSize);
            createMySQLVersion("mysql_node_complex_" + i, Collections.singletonList(parent));
        }
        log.info("[MySQL-Complex] Generated complex tree data (range {}..{}). Count={}",
                COMPLEX_START, COMPLEX_END, (COMPLEX_END - COMPLEX_START + 1));
    }


    /* ================================================================= */
    /*                       Neo4j 데이터 생성 메서드                     */
    /* ================================================================= */

    // ------------------- Neo4j 체인: [1..scale] -------------------
    public void generateNeo4jChainData() {
        List<VersionNodeBatchDTO> dtos = new ArrayList<>(CHAIN_END - CHAIN_START + 1);

        // 루트
        dtos.add(new VersionNodeBatchDTO(String.valueOf(CHAIN_START),
                "neo4j_root_chain_" + CHAIN_START,
                Collections.emptyList()));

        for (long i = CHAIN_START + 1; i <= CHAIN_END; i++) {
            String nodeId = String.valueOf(i);
            String parentId = String.valueOf(i - 1);
            dtos.add(new VersionNodeBatchDTO(
                    nodeId,
                    "neo4j_v_chain_" + i,
                    Collections.singletonList(parentId)));
        }

        int inserted = bulkInsertNeo4jNodes(dtos);
        log.info("[Neo4j-Chain] Inserted {} nodes (range {}..{})", inserted, CHAIN_START, CHAIN_END);
    }

    // ------------------- Neo4j 이진 트리: [scale+1..2*scale] -------------------
    public void generateNeo4jBinaryTreeData() {
        int count = (BINARY_END - BINARY_START + 1);
        List<VersionNodeBatchDTO> dtos = new ArrayList<>(count);

        // 루트
        dtos.add(new VersionNodeBatchDTO(String.valueOf(BINARY_START),
                "neo4j_root_tree_" + BINARY_START,
                Collections.emptyList()));

        for (long i = BINARY_START + 1; i <= BINARY_END; i++) {
            long offset = i - BINARY_START;   // 1..(BINARY_END - BINARY_START)
            long parentOffset = offset / 2;  // 부모의 offset
            long parentIdLong = BINARY_START + parentOffset;
            String parentId = String.valueOf(parentIdLong);

            dtos.add(new VersionNodeBatchDTO(
                    String.valueOf(i),
                    "neo4j_node_tree_" + i,
                    Collections.singletonList(parentId)));
        }

        int inserted = bulkInsertNeo4jNodes(dtos);
        log.info("[Neo4j-Tree] Inserted {} nodes (range {}..{})", inserted, BINARY_START, BINARY_END);
    }

    // ------------------- Neo4j 복합 트리: [2*scale+1..3*scale] -------------------
    public void generateNeo4jComplexTreeData() {
        int count = (COMPLEX_END - COMPLEX_START + 1);
        List<VersionNodeBatchDTO> dtos = new ArrayList<>(count);
        Random rand = new Random();

        // 루트
        dtos.add(new VersionNodeBatchDTO(String.valueOf(COMPLEX_START),
                "neo4j_root_complex_" + COMPLEX_START,
                Collections.emptyList()));

        for (long i = COMPLEX_START + 1; i <= COMPLEX_END; i++) {
            long rangeSize = (i - 1) - COMPLEX_START + 1;
            if (rangeSize < 1) rangeSize = 1;

            long parent = COMPLEX_START + rand.nextInt((int)rangeSize);

            dtos.add(new VersionNodeBatchDTO(
                    String.valueOf(i),
                    "neo4j_node_complex_" + i,
                    Collections.singletonList(String.valueOf(parent))));
        }

        int inserted = bulkInsertNeo4jNodes(dtos);
        log.info("[Neo4j-Complex] Inserted {} nodes (range {}..{})", inserted, COMPLEX_START, COMPLEX_END);
    }

}
