package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.VersionEntity;
import com.gdbrdb.test.entity.neo4j.VersionNodeNew;
import com.gdbrdb.test.repository.mysql.VersionRepository;
import com.gdbrdb.test.repository.neo4j.VersionNodeNewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    // MySQL 관련 리포지토리 (VersionEntity 사용)
    private final VersionRepository mysqlRepo;
    // Neo4j 관련 리포지토리 (VersionNodeNew 사용)
    private final VersionNodeNewRepository neo4jNewRepo;

    /* ======================= */
    /*       MySQL PART        */
    /* ======================= */

    /**
     * MySQL 버전 생성 (다중 부모 지원)
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
     * MySQL: 두 버전의 최하단 공통 조상(LCA) 계산 (단순 예시)
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
     * MySQL: EXPLAIN 실행 계획 조회 (단순 SELECT 쿼리)
     */
    public List<Object[]> logAndGetMySQLExplainPlan(Long id) {
        List<Object[]> plan = mysqlRepo.explainSelectById(id);
        log.info("[MySQL EXPLAIN] Plan for id {}: {}", id, plan);
        return plan;
    }

    /* 데이터 조회 헬퍼 (MySQL) */
    public Long getLastMySQLChainId() {
        // 체인 데이터: content가 "mysql_root_chain" 또는 "mysql_v_chain_" 로 시작하는 데이터 중 가장 큰 id
        return mysqlRepo.findAll().stream()
                .filter(v -> v.getContent().startsWith("mysql_root_chain") ||
                        v.getContent().startsWith("mysql_v_chain_"))
                .map(VersionEntity::getId)
                .max(Long::compareTo)
                .orElse(null);
    }

    public Long getLastMySQLBinaryTreeId() {
        // 이진 트리 데이터: content가 "mysql_root_tree" 또는 "mysql_node_tree_" 로 시작
        return mysqlRepo.findAll().stream()
                .filter(v -> v.getContent().startsWith("mysql_root_tree") ||
                        v.getContent().startsWith("mysql_node_tree_"))
                .map(VersionEntity::getId)
                .max(Long::compareTo)
                .orElse(null);
    }

    public Long getLastMySQLComplexTreeId() {
        // 복합 트리 데이터: content가 "mysql_root_complex" 또는 "mysql_node_complex_" 로 시작
        return mysqlRepo.findAll().stream()
                .filter(v -> v.getContent().startsWith("mysql_root_complex") ||
                        v.getContent().startsWith("mysql_node_complex_"))
                .map(VersionEntity::getId)
                .max(Long::compareTo)
                .orElse(null);
    }

    public Long getMySQLChainMidId() {
        List<VersionEntity> list = mysqlRepo.findAll().stream()
                .filter(v -> v.getContent().startsWith("mysql_v_chain_") ||
                        v.getContent().startsWith("mysql_root_chain"))
                .sorted(Comparator.comparing(VersionEntity::getId))
                .collect(Collectors.toList());
        return list.get(list.size() / 2).getId();
    }

    public Long getMySQLTreeMidId() {
        List<VersionEntity> list = mysqlRepo.findAll().stream()
                .filter(v -> v.getContent().startsWith("mysql_node_tree_") ||
                        v.getContent().startsWith("mysql_root_tree"))
                .sorted(Comparator.comparing(VersionEntity::getId))
                .collect(Collectors.toList());
        return list.get(list.size() / 2).getId();
    }

    /* ======================= */
    /*      Neo4j (VersionNew) PART         */
    /* ======================= */

    /**
     * Neo4j (VersionNew): 버전 노드 생성 (다중 부모 지원, 사용자 정의 nodeId 사용)
     */
    public VersionNodeNew createNeo4jVersion(String content, List<String> parentNodeIds) {
        VersionNodeNew newNode = new VersionNodeNew();
        newNode.setContent(content);
        if (parentNodeIds != null && !parentNodeIds.isEmpty()) {
            List<VersionNodeNew> parents = neo4jNewRepo.findAllById(parentNodeIds);
            parents.forEach(newNode::addParent);
        }
        return neo4jNewRepo.save(newNode);
    }

    /**
     * Neo4j (VersionNew): 특정 노드의 모든 조상 조회
     */
    public List<VersionNodeNew> getNeo4jAllAncestors(String nodeId) {
        return neo4jNewRepo.findAllAncestors(nodeId);
    }

    /**
     * Neo4j (VersionNew): 두 노드의 최하단 공통 조상(LCA) 계산 (단순 예시)
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
        // 단순 비교: 사전순 최대 (원하는 로직에 맞게 수정 가능)
        String candidateId = aIds.stream().max(String::compareTo).orElse(null);
        return candidateId != null ? neo4jNewRepo.findById(candidateId).orElse(null) : null;
    }

    /**
     * Neo4j (VersionNew): EXPLAIN 실행 계획 조회
     */
    public String logAndGetNeo4jExplainPlan(String nodeId) {
        String plan = neo4jNewRepo.explainAncestors(nodeId);
        log.info("[Neo4j EXPLAIN] Plan for nodeId {}: {}", nodeId, plan);
        return plan;
    }

    /* 데이터 조회 헬퍼 (Neo4j) */
    public String getLastNeo4jChainNodeId() {
        List<VersionNodeNew> list = neo4jNewRepo.findAll().stream()
                .filter(n -> n.getContent().startsWith("neo4j_root_chain") ||
                        n.getContent().startsWith("neo4j_v_chain_"))
                .sorted(Comparator.comparing(VersionNodeNew::getNodeId))
                .collect(Collectors.toList());
        return list.isEmpty() ? null : list.get(list.size() - 1).getNodeId();
    }

    public String getLastNeo4jBinaryTreeNodeId() {
        List<VersionNodeNew> list = neo4jNewRepo.findAll().stream()
                .filter(n -> n.getContent().startsWith("neo4j_root_tree") ||
                        n.getContent().startsWith("neo4j_node_tree_"))
                .sorted(Comparator.comparing(VersionNodeNew::getNodeId))
                .collect(Collectors.toList());
        return list.isEmpty() ? null : list.get(list.size() - 1).getNodeId();
    }

    public String getLastNeo4jComplexTreeNodeId() {
        List<VersionNodeNew> list = neo4jNewRepo.findAll().stream()
                .filter(n -> n.getContent().startsWith("neo4j_root_complex") ||
                        n.getContent().startsWith("neo4j_node_complex_"))
                .sorted(Comparator.comparing(VersionNodeNew::getNodeId))
                .collect(Collectors.toList());
        return list.isEmpty() ? null : list.get(list.size() - 1).getNodeId();
    }

    public VersionNodeNew getNeo4jChainMidNode() {
        List<VersionNodeNew> list = neo4jNewRepo.findAll().stream()
                .filter(n -> n.getContent().startsWith("neo4j_v_chain_") ||
                        n.getContent().startsWith("neo4j_root_chain"))
                .sorted(Comparator.comparing(VersionNodeNew::getNodeId))
                .collect(Collectors.toList());
        return list.get(list.size() / 2);
    }

    public VersionNodeNew getNeo4jTreeMidNode() {
        List<VersionNodeNew> list = neo4jNewRepo.findAll().stream()
                .filter(n -> n.getContent().startsWith("neo4j_node_tree_") ||
                        n.getContent().startsWith("neo4j_root_tree"))
                .sorted(Comparator.comparing(VersionNodeNew::getNodeId))
                .collect(Collectors.toList());
        return list.get(list.size() / 2);
    }

    /**
     * 간단한 메모리 사용량 측정 헬퍼 (실행 전후 메모리 차이 반환)
     */
    public long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /* ============================ */
    /*      DB 데이터 생성 메서드      */
    /* ============================ */

    // [MySQL] 체인 데이터 생성 (연결형)
    public List<Long> generateMySQLChainData(int scale) {
        List<Long> ids = new ArrayList<>();
        VersionEntity root = createMySQLVersion("mysql_root_chain", null);
        ids.add(root.getId());
        for (int i = 2; i <= scale; i++) {
            VersionEntity v = createMySQLVersion("mysql_v_chain_" + i,
                    Collections.singletonList(ids.get(ids.size() - 1)));
            ids.add(v.getId());
        }
        log.info("[MySQL-Chain] Inserted chain of {} nodes.", scale);
        return ids;
    }

    // [Neo4j] 체인 데이터 생성 (VersionNew)
    public List<String> generateNeo4jChainData(int scale) {
        List<String> ids = new ArrayList<>();
        VersionNodeNew root = createNeo4jVersion("neo4j_root_chain", null);
        ids.add(root.getNodeId());
        for (int i = 2; i <= scale; i++) {
            VersionNodeNew node = createNeo4jVersion("neo4j_v_chain_" + i,
                    Collections.singletonList(ids.get(ids.size() - 1)));
            ids.add(node.getNodeId());
        }
        log.info("[Neo4j-Chain] Inserted chain of {} nodes.", scale);
        return ids;
    }

    // [MySQL] 이진 트리 데이터 생성 (Binary Tree)
    public List<Long> generateMySQLBinaryTreeData(int scale) {
        List<Long> ids = new ArrayList<>();
        VersionEntity root = createMySQLVersion("mysql_root_tree", null);
        ids.add(root.getId());
        for (int i = 2; i <= scale; i++) {
            int parentIndex = (i / 2) - 1;
            Long parentId = ids.get(parentIndex);
            VersionEntity node = createMySQLVersion("mysql_node_tree_" + i,
                    Collections.singletonList(parentId));
            ids.add(node.getId());
        }
        log.info("[MySQL-Tree] Inserted binary tree of {} nodes.", scale);
        return ids;
    }

    // [Neo4j] 이진 트리 데이터 생성 (Binary Tree, VersionNew)
    public List<String> generateNeo4jBinaryTreeData(int scale) {
        List<String> ids = new ArrayList<>();
        VersionNodeNew root = createNeo4jVersion("neo4j_root_tree", null);
        ids.add(root.getNodeId());
        for (int i = 2; i <= scale; i++) {
            int parentIndex = (i / 2) - 1;
            String parentId = ids.get(parentIndex);
            VersionNodeNew node = createNeo4jVersion("neo4j_node_tree_" + i,
                    Collections.singletonList(parentId));
            ids.add(node.getNodeId());
        }
        log.info("[Neo4j-Tree] Inserted binary tree of {} nodes.", scale);
        return ids;
    }

    // [MySQL] 복합 트리 데이터 생성 (불규칙 트리)
    public List<Long> generateMySQLComplexTreeData(int scale) {
        List<Long> ids = new ArrayList<>();
        VersionEntity root = createMySQLVersion("mysql_root_complex", null);
        ids.add(root.getId());
        Random rand = new Random();
        for (int i = 2; i <= scale; i++) {
            int pickRange = Math.max(1, (int)(ids.size() * 0.7));
            int startIndex = Math.max(0, ids.size() - pickRange);
            List<Long> subList = ids.subList(startIndex, ids.size());
            Long parentId = subList.get(rand.nextInt(subList.size()));
            VersionEntity node = createMySQLVersion("mysql_node_complex_" + i,
                    Collections.singletonList(parentId));
            ids.add(node.getId());
        }
        log.info("[MySQL-Complex] Inserted complex tree of {} nodes.", scale);
        return ids;
    }

    // [Neo4j] 복합 트리 데이터 생성 (불규칙 트리, VersionNew)
    public List<String> generateNeo4jComplexTreeData(int scale) {
        List<String> ids = new ArrayList<>();
        VersionNodeNew root = createNeo4jVersion("neo4j_root_complex", null);
        ids.add(root.getNodeId());
        Random rand = new Random();
        for (int i = 2; i <= scale; i++) {
            int pickRange = Math.max(1, (int)(ids.size() * 0.7));
            int startIndex = Math.max(0, ids.size() - pickRange);
            List<String> subList = ids.subList(startIndex, ids.size());
            String parentId = subList.get(rand.nextInt(subList.size()));
            VersionNodeNew node = createNeo4jVersion("neo4j_node_complex_" + i,
                    Collections.singletonList(parentId));
            ids.add(node.getNodeId());
        }
        log.info("[Neo4j-Complex] Inserted complex tree of {} nodes.", scale);
        return ids;
    }
}
