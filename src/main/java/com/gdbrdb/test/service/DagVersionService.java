package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.DagVersionEntity;
import com.gdbrdb.test.entity.neo4j.DagVersionNode;
import com.gdbrdb.test.repository.mysql.DagVersionRepository;
import com.gdbrdb.test.repository.neo4j.DagVersionNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DagVersionService {

    private final DagVersionRepository dagMysqlRepo;
    private final DagVersionNodeRepository dagNeoRepo;

    private static final int DAG_SCALE = 10000;

    private static final List<String> AUTHORS = List.of("kanguk", "youngjin", "chulsu", "younghee");
    private static final List<String> SAMPLE_TITLES = List.of(
            "hwp 파일 수정", "pdf 파일 수정", "문서 작업", "기획서 수정", "ppt 슬라이드 변경"
    );

    /**
     * 메모리 사용량 측정 (원래 VersionService 등과 비슷한 형태)
     */
    public long getUsedMemory() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    /**
     * 1) MySQL + 2) Neo4j에 DAG 데이터 1만개 생성 (다중 부모 허용)
     *  - 사이클 방지: 부모는 자기보다 ID가 작은 노드들 중에서 선택
     *    + 이미 조상에 있으면 skip
     */
    public void generateDagData() {
        long start = System.currentTimeMillis();
        long startMem = getUsedMemory();

        // 1) MySQL DAG 생성
        generateMySqlDag();

        long afterMy = System.currentTimeMillis();
        long memMy = getUsedMemory();
        System.out.println("[MySQL DAG] time=" + (afterMy - start) + "ms, memUsed=" + (memMy - startMem));

        // 2) Neo4j DAG 생성
        generateNeo4jDag();

        long end = System.currentTimeMillis();
        long endMem = getUsedMemory();
        System.out.println("[Neo4j DAG] time=" + (end - afterMy) + "ms, memUsed=" + (endMem - memMy));

        System.out.println("[DAG total] time=" + (end - start) + "ms, memUsed=" + (endMem - startMem));
    }

    private void generateMySqlDag() {
        // ID -> entity 캐시
        List<DagVersionEntity> allCreated = new ArrayList<>();
        allCreated.add(null); // index 0 비우기 (편의상)

        // 우선 루트(1번) 만들어두기
        DagVersionEntity root = createMysqlOne(1L, Collections.emptyList());
        allCreated.add(root);

        // i=2..DAG_SCALE
        for (long i = 2; i <= DAG_SCALE; i++) {
            // 부모 후보를 랜덤하게 1~3개 고르되, 이미 조상이면 안 됨
            // (여기서는 간단히 "i보다 작은 애들 중 1~3개"를 고르고,
            //  중복/사이클은 무시 or 최소한으로 체크)
            List<Long> parents = pickRandomParents(i, allCreated);

            // 엔티티 생성
            DagVersionEntity newEnt = createMysqlOne(i, parents);
            allCreated.add(newEnt);
        }
    }

    private DagVersionEntity createMysqlOne(Long nodeId, List<Long> parentIds) {
        DagVersionEntity ent = new DagVersionEntity();
        ent.setTitle(randTitle());
        ent.setContent("임의 내용.. nodeId=" + nodeId);
        ent.setAuthor(randAuthor());
        ent.setCreatedTime(randTime2025FebMar());

        if (!parentIds.isEmpty()) {
            List<DagVersionEntity> parentEnts = dagMysqlRepo.findAllById(parentIds);
            for (DagVersionEntity p : parentEnts) {
                ent.addParent(p);
            }
        }

        return dagMysqlRepo.save(ent);
    }

    // 사이클 방지 or 최소화 로직 (간단히 구현)
    private List<Long> pickRandomParents(Long childId, List<DagVersionEntity> allEnts) {
        Random rand = new Random();
        int parentCount = rand.nextInt(3) + 1; // 1~3
        Set<Long> chosen = new HashSet<>();

        // 단순 버전:
        // childId보다 작은 범위에서 parentCount개를 랜덤선택
        // (사실 엄밀히 '조상 검사'를 해야 하는데, 여기선 예시로 간단화)
        long maxParentRange = childId - 1;
        if (maxParentRange < 1) return Collections.emptyList();

        // 최대 parentCount번 시도
        // (사실 childId가 2,3이면 parent 후보가 별로 없어서 중복날 수도)
        for (int i = 0; i < parentCount; i++) {
            long p = 1 + rand.nextLong(maxParentRange);
            chosen.add(p);
        }

        // 여기서 더 엄밀히 "사이클 체크"를 하려면,
        // chosen 중에 이미 childId의 조상인 노드가 있는지 DFS 등으로 검사 가능.
        // 데모 목적이므로 간단히 skip.

        return new ArrayList<>(chosen);
    }

    private void generateNeo4jDag() {
        // 루트(1)부터 10000까지, parents를 동일 로직으로 pick, batch insert
        List<Map<String,Object>> batch = new ArrayList<>(DAG_SCALE);

        // i=1 => 루트
        Map<String,Object> rootMap = new HashMap<>();
        rootMap.put("nodeId", "1");
        rootMap.put("title", randTitle());
        rootMap.put("content", "임의 내용.. nodeId=1");
        rootMap.put("author", randAuthor());
        rootMap.put("createdTime", randTime2025FebMar().toString());
        rootMap.put("parents", Collections.emptyList());
        batch.add(rootMap);

        // 2..DAG_SCALE
        Random rand = new Random();
        for (long i = 2; i <= DAG_SCALE; i++) {
            Map<String,Object> row = new HashMap<>();
            row.put("nodeId", String.valueOf(i));
            row.put("title", randTitle());
            row.put("content", "임의 내용.. nodeId=" + i);
            row.put("author", randAuthor());
            row.put("createdTime", randTime2025FebMar().toString());

            int parentCount = rand.nextInt(3) + 1; // 1~3
            long maxParent = i - 1;
            if (maxParent < 1) {
                row.put("parents", Collections.emptyList());
            } else {
                Set<String> parents = new HashSet<>();
                for (int k = 0; k < parentCount; k++) {
                    long p = 1 + rand.nextLong(maxParent);
                    parents.add(String.valueOf(p));
                }
                row.put("parents", new ArrayList<>(parents));
            }
            batch.add(row);
        }

        dagNeoRepo.bulkInsertNodes(batch);
    }

    /* 유틸 메서드들 */
    private String randTitle() {
        Random rand = new Random();
        return SAMPLE_TITLES.get(rand.nextInt(SAMPLE_TITLES.size()));
    }

    private String randAuthor() {
        Random rand = new Random();
        return AUTHORS.get(rand.nextInt(AUTHORS.size()));
    }

    private LocalDateTime randTime2025FebMar() {
        // 2025년 2월 1일 ~ 3월 31일 사이
        LocalDateTime start = LocalDateTime.of(2025, Month.FEBRUARY, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, Month.MARCH, 31, 23, 59);

        long startSec = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endSec = end.toEpochSecond(java.time.ZoneOffset.UTC);

        long diff = endSec - startSec;
        Random r = new Random();
        long randSec = startSec + (long)(r.nextDouble() * diff);

        return LocalDateTime.ofEpochSecond(randSec, 0, java.time.ZoneOffset.UTC);
    }
}
