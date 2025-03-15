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
 * - scale에 따라 (체인, 이진, 복합) 각각:
 *   [1..scale], [scale+1..2*scale], [2*scale+1..3*scale] 범위로 생성
 * - 체인, 이진은 이미 MySQL/Neo4j 동일 구조가 자동으로 보장됨
 * - 복합(Complex)은 난수로 parent를 결정 ->
 *   두 DB에서 동일한 난수를 쓰도록 아래 generateComplexTreeData()를 통해 한 번만 계산 후 삽입
 */
@Service
@RequiredArgsConstructor
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    /** 원하는 규모(scale) */
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

    // MySQL / Neo4j 리포지토리
    private final VersionRepository mysqlRepo;
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

    /** MySQL: 모든 조상 **엔티티** 조회 */
    public List<VersionEntity> getMySQLAllAncestorEntities(Long versionId) {
        return mysqlRepo.findAllAncestorEntities(versionId);
    }

    /**
     * MySQL: LCA(최하단 공통 조상) (엔티티 기반)
     * - 여기서는 단순히 'ID가 가장 큰 조상'을 선정하는 방식 그대로 두지만,
     *   만약 '최신 버전'의 의미가 내용적으로 달라진다면
     *   다른 기준(날짜가 최신 등)을 쓸 수도 있습니다.
     */
    public VersionEntity getMySQLLowestCommonAncestorEntity(Long idA, Long idB) {
        // A의 조상
        List<VersionEntity> ancestorsA = getMySQLAllAncestorEntities(idA);
        // A 자신도 포함
        ancestorsA.add(mysqlRepo.findById(idA).orElse(null));
        // Map으로 (id -> entity) 빠른 조회
        Map<Long, VersionEntity> mapA = new HashMap<>();
        for (VersionEntity a : ancestorsA) {
            if (a != null) {
                mapA.put(a.getId(), a);
            }
        }

        // B의 조상
        List<VersionEntity> ancestorsB = getMySQLAllAncestorEntities(idB);
        // B 자신도 포함
        ancestorsB.add(mysqlRepo.findById(idB).orElse(null));

        // 교집합 중 ID가 가장 큰 것을 찾음
        VersionEntity lca = null;
        for (VersionEntity b : ancestorsB) {
            if (b != null && mapA.containsKey(b.getId())) {
                // 교집합 노드
                if (lca == null || b.getId() > lca.getId()) {
                    lca = b;
                }
            }
        }
        return lca;
    }

    /**
     * MySQL: 자손 **엔티티** 조회
     */
    public List<VersionEntity> getMySQLAllDescendantEntities(Long versionId) {
        return mysqlRepo.findAllDescendantEntities(versionId);
    }

    /** MySQL: EXPLAIN */
    public List<Object[]> logAndGetMySQLExplainPlan(Long id) {
        List<Object[]> plan = mysqlRepo.explainSelectById(id);
        log.info("[MySQL EXPLAIN] Plan for id {}: {}", id, plan);
        return plan;
    }

    /* ======================= */
    /*    Neo4j PART           */
    /* ======================= */

    public VersionNodeNewRepository getNeo4jNewRepo() {
        return this.neo4jNewRepo;
    }

    /** 단건 삽입 (주의: 대량엔 bulkInsert 사용 권장) */
    public VersionNodeNew createNeo4jVersion(String nodeId, String content, List<String> parentNodeIds) {
        VersionNodeNew newNode = new VersionNodeNew(nodeId, content);
        if (parentNodeIds != null && !parentNodeIds.isEmpty()) {
            List<VersionNodeNew> parents = neo4jNewRepo.findAllById(parentNodeIds);
            parents.forEach(newNode::addParent);
        }
        return neo4jNewRepo.save(newNode);
    }

    /** nodeId 없이 생성 (UUID) */
    public VersionNodeNew createNeo4jVersion(String content, List<String> parentNodeIds) {
        return createNeo4jVersion(UUID.randomUUID().toString(), content, parentNodeIds);
    }

    /** 모든 조상 */
    public List<VersionNodeNew> getNeo4jAllAncestors(String nodeId) {
        return neo4jNewRepo.findAllAncestors(nodeId);
    }

    /** LCA */
    public VersionNodeNew getNeo4jLowestCommonAncestor(String idA, String idB) {
        // 1) 조상 조회 (이제는 INCOMING 방향이므로 정상)
        List<VersionNodeNew> ancestorsA = getNeo4jAllAncestors(idA);
        // nodeA 자체도 조상 후보에 넣고 싶으면 ID만 추가
        // (엔티티 풀로딩 방지 목적)
        // ancestorsA.add(new VersionNodeNew(idA, null));

        List<VersionNodeNew> ancestorsB = getNeo4jAllAncestors(idB);
        // ancestorsB.add(new VersionNodeNew(idB, null));

        // 2) 교집합
        Set<String> aIds = ancestorsA.stream().map(VersionNodeNew::getNodeId).collect(Collectors.toSet());
        aIds.add(idA);

        Set<String> bIds = ancestorsB.stream().map(VersionNodeNew::getNodeId).collect(Collectors.toSet());
        bIds.add(idB);

        aIds.retainAll(bIds);
        if (aIds.isEmpty()) return null;

        // 3) "가장 큰 ID" 선택
        String candidateId = aIds.stream().max(String::compareTo).orElse(null);
        if (candidateId == null) return null;

        // 4) 무한 로딩 방지를 위해, 바로 stub 리턴 (or shallow query)
        return new VersionNodeNew(candidateId, "LCA-stub");
    }


    /** EXPLAIN */
    public String logAndGetNeo4jExplainPlan(String nodeId) {
        String plan = neo4jNewRepo.explainAncestors(nodeId);
        log.info("[Neo4j EXPLAIN] Plan for nodeId {}: {}", nodeId, plan);
        return plan;
    }

    /** 메모리 사용량 */
    public long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /** Neo4j Batch Insert (UNWIND) */
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
    /*     MySQL 데이터 생성: 체인, 이진 (동일 구조 보장), 복합(랜덤)      */
    /* ================================================================= */

    // ------------------- MySQL 체인: [1..scale] -------------------
    public void generateMySQLChainData() {
        createMySQLVersion("mysql_root_chain_" + CHAIN_START, null);
        for (long i = CHAIN_START + 1; i <= CHAIN_END; i++) {
            createMySQLVersion("mysql_v_chain_" + i, Collections.singletonList(i - 1));
        }
        log.info("[MySQL-Chain] range {}..{}, count={}", CHAIN_START, CHAIN_END, (CHAIN_END - CHAIN_START + 1));
    }

    // ------------------- MySQL 이진 트리: [scale+1..2*scale] -------------------
    public void generateMySQLBinaryTreeData() {
        createMySQLVersion("mysql_root_tree_" + BINARY_START, null);
        for (long i = BINARY_START + 1; i <= BINARY_END; i++) {
            long offset = i - BINARY_START;
            long parentOffset = offset / 2;
            long parentId = BINARY_START + parentOffset;
            createMySQLVersion("mysql_node_tree_" + i, Collections.singletonList(parentId));
        }
        log.info("[MySQL-Tree] range {}..{}, count={}", BINARY_START, BINARY_END, (BINARY_END - BINARY_START + 1));
    }

    /* ================================================================= */
    /*   "공통 로직"을 사용해 MySQL, Neo4j 복합 트리를 동시 생성하는 메서드 */
    /* ================================================================= */

    public void generateComplexTreeData() {
        // 1) 먼저 "child->parent" 관계를 랜덤 생성 (공용)
        Random rand = new Random();
        Map<Long, Long> childToParent = new HashMap<>();

        // 루트 (COMPLEX_START) => 부모 없음
        for (long i = COMPLEX_START + 1; i <= COMPLEX_END; i++) {
            long rangeSize = (i - 1) - COMPLEX_START + 1;
            if (rangeSize < 1) rangeSize = 1;
            long parent = COMPLEX_START + rand.nextInt((int) rangeSize);
            childToParent.put(i, parent);
        }

        // 2) MySQL에 삽입
        createMySQLVersion("mysql_root_complex_" + COMPLEX_START, null);
        for (long i = COMPLEX_START + 1; i <= COMPLEX_END; i++) {
            long p = childToParent.get(i);
            createMySQLVersion("mysql_node_complex_" + i, Collections.singletonList(p));
        }
        log.info("[MySQL-Complex] Inserted range {}..{}, count={}",
                COMPLEX_START, COMPLEX_END, (COMPLEX_END - COMPLEX_START + 1));

        // 3) Neo4j에 삽입 (동일 parent)
        List<VersionNodeBatchDTO> dtos = new ArrayList<>();
        // 루트
        dtos.add(new VersionNodeBatchDTO(
                String.valueOf(COMPLEX_START),
                "neo4j_root_complex_" + COMPLEX_START,
                Collections.emptyList()
        ));

        for (long i = COMPLEX_START + 1; i <= COMPLEX_END; i++) {
            long p = childToParent.get(i);
            dtos.add(new VersionNodeBatchDTO(
                    String.valueOf(i),
                    "neo4j_node_complex_" + i,
                    Collections.singletonList(String.valueOf(p))
            ));
        }

        int insertedNeo = bulkInsertNeo4jNodes(dtos);
        log.info("[Neo4j-Complex] Inserted range {}..{}, count={}",
                COMPLEX_START, COMPLEX_END, insertedNeo);
    }

    /* ================================================================= */
    /*   Neo4j 체인, 이진 (별도)                                        */
    /* ================================================================= */

    // ------------------- Neo4j 체인: [1..scale] -------------------
    public void generateNeo4jChainData() {
        List<VersionNodeBatchDTO> dtos = new ArrayList<>();
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
        log.info("[Neo4j-Chain] range {}..{}, count={}", CHAIN_START, CHAIN_END, inserted);
    }

    // ------------------- Neo4j 이진 트리: [scale+1..2*scale] -------------------
    public void generateNeo4jBinaryTreeData() {
        List<VersionNodeBatchDTO> dtos = new ArrayList<>();
        dtos.add(new VersionNodeBatchDTO(String.valueOf(BINARY_START),
                "neo4j_root_tree_" + BINARY_START,
                Collections.emptyList()));

        for (long i = BINARY_START + 1; i <= BINARY_END; i++) {
            long offset = i - BINARY_START;
            long parentOffset = offset / 2;
            long parentIdLong = BINARY_START + parentOffset;

            dtos.add(new VersionNodeBatchDTO(
                    String.valueOf(i),
                    "neo4j_node_tree_" + i,
                    Collections.singletonList(String.valueOf(parentIdLong))));
        }

        int inserted = bulkInsertNeo4jNodes(dtos);
        log.info("[Neo4j-Tree] range {}..{}, count={}", BINARY_START, BINARY_END, inserted);
    }
}
