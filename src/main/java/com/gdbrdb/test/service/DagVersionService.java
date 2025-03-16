package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.DagVersionEntity;
import com.gdbrdb.test.entity.neo4j.DagVersionNode;
import com.gdbrdb.test.repository.mysql.DagVersionRepository;
import com.gdbrdb.test.repository.neo4j.DagVersionNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    public void generateDagData() {
        long start = System.currentTimeMillis();
        long startMem = getUsedMemory();

        // 1) 미리 "child->parents" 관계 전부 계산
        Map<Long, List<Long>> childToParents = buildChildParentMap();

        // 2) MySQL 삽입 (동일 구조)
        insertMySqlDag(childToParents);
        long afterMy = System.currentTimeMillis();
        long memMy = getUsedMemory();
        System.out.println("[MySQL DAG] time=" + (afterMy - start) + "ms, memUsed=" + (memMy - startMem));

        // 3) Neo4j 삽입 (동일 구조)
        insertNeo4jDag(childToParents);
        long end = System.currentTimeMillis();
        long endMem = getUsedMemory();
        System.out.println("[Neo4j DAG] time=" + (end - afterMy) + "ms, memUsed=" + (endMem - memMy));
        System.out.println("[DAG total] time=" + (end - start) + "ms, memUsed=" + (endMem - startMem));
    }

    /**
     * 1..DAG_SCALE 노드에 대해, 각 child -> (최대 1~3개) 부모 IDs
     * (사이클 최소화 로직은 동일)
     */
    private Map<Long, List<Long>> buildChildParentMap() {
        Map<Long, List<Long>> childToParents = new LinkedHashMap<>();
        childToParents.put(1L, Collections.emptyList()); // 루트(1)

        Random rand = new Random();
        // i=2..DAG_SCALE
        for (long i = 2; i <= DAG_SCALE; i++) {
            long maxParentRange = i - 1;
            if (maxParentRange < 1) {
                childToParents.put(i, Collections.emptyList());
                continue;
            }
            int parentCount = rand.nextInt(3) + 1; // 1~3
            Set<Long> chosen = new HashSet<>();
            for (int c = 0; c < parentCount; c++) {
                long p = 1 + rand.nextLong(maxParentRange);
                chosen.add(p);
            }
            List<Long> parentList = new ArrayList<>(chosen);
            childToParents.put(i, parentList);
        }
        return childToParents;
    }

    /**
     * MySQL에 동일 구조 삽입
     */
    private void insertMySqlDag(Map<Long, List<Long>> childToParents) {
        // 캐싱: ID->entity
        Map<Long, DagVersionEntity> cache = new HashMap<>();
        // 1..DAG_SCALE
        for (long i = 1; i <= DAG_SCALE; i++) {
            DagVersionEntity ent = new DagVersionEntity();
            ent.setTitle(randTitle());
            ent.setContent("임의 내용.. nodeId=" + i);
            ent.setAuthor(randAuthor());
            // "2025-02-15T17:15:08Z" 형태가 아니라, MySQL은 LocalDateTime 그대로
            ent.setCreatedTime(randTime2025FebMar());

            // 부모 설정
            List<Long> parents = childToParents.get(i);
            if (parents != null && !parents.isEmpty()) {
                List<DagVersionEntity> parentEnts = new ArrayList<>();
                for (Long pId : parents) {
                    DagVersionEntity pEnt = cache.get(pId);  // 이미 insert된
                    if (pEnt != null) {
                        parentEnts.add(pEnt);
                    }
                }
                for (DagVersionEntity p : parentEnts) {
                    ent.addParent(p);
                }
            }
            DagVersionEntity saved = dagMysqlRepo.save(ent);
            cache.put(i, saved);
        }
    }

    /**
     * Neo4j에 동일 구조 삽입
     * - 여기서 createdTime은 "ISO8601 + Z" 형태로 저장
     * - bulkInsertNodes 쿼리에서 datetime(...) 변환
     */
    private void insertNeo4jDag(Map<Long, List<Long>> childToParents) {
        List<Map<String, Object>> batch = new ArrayList<>(DAG_SCALE);

        for (long i = 1; i <= DAG_SCALE; i++) {
            Map<String,Object> row = new HashMap<>();
            row.put("nodeId", String.valueOf(i));
            row.put("title", randTitle());
            row.put("content", "임의 내용.. nodeId=" + i);
            row.put("author", randAuthor());

            // LocalDateTime -> OffsetDateTime(UTC), then to string with 'Z'
            LocalDateTime dt = randTime2025FebMar();
            OffsetDateTime odt = dt.atOffset(ZoneOffset.UTC);  // 2025-03-15T04:09:10Z
            String dateTimeStr = odt.toString(); // e.g. "2025-03-15T04:09:10Z"
            row.put("createdTime", dateTimeStr);

            List<Long> parents = childToParents.get(i);
            if (parents == null || parents.isEmpty()) {
                row.put("parents", Collections.emptyList());
            } else {
                List<String> parentStr = new ArrayList<>();
                for (Long p : parents) {
                    parentStr.add(String.valueOf(p));
                }
                row.put("parents", parentStr);
            }
            batch.add(row);
        }
        dagNeoRepo.bulkInsertNodes(batch);
    }

    // (아래 유틸, 기존과 동일)
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
        LocalDateTime start = LocalDateTime.of(2025, 2, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 3, 31, 23, 59);
        long startSec = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endSec = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long diff = endSec - startSec;
        Random r = new Random();
        long randSec = startSec + (long)(r.nextDouble() * diff);
        return LocalDateTime.ofEpochSecond(randSec, 0, java.time.ZoneOffset.UTC);
    }

    public long getUsedMemory() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }
}
