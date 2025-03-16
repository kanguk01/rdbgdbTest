package com.gdbrdb.test.service;

import com.gdbrdb.test.entity.mysql.DagVersionEntity;
import com.gdbrdb.test.entity.neo4j.DagVersionNode;
import com.gdbrdb.test.repository.mysql.DagVersionRepository;
import com.gdbrdb.test.repository.neo4j.DagVersionNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DagScenarioService {

    private final DagVersionRepository mysqlRepo;
    private final DagVersionNodeRepository neoRepo;

    // 시나리오 A) 조상 중 특정 author & title
    public List<DagVersionEntity> findMySqlAncestorsByAuthorTitle(Long startId, String author, String titlePart) {
        return mysqlRepo.findAncestorsByAuthorAndTitle(startId, author, titlePart);
    }

    public List<DagVersionNode> findNeoAncestorsByAuthorTitle(String startId, String author, String titlePart) {
        return neoRepo.findAncestorsByAuthorAndTitle(startId, author, titlePart);
    }

    // 시나리오 B) 자손 중 특정 시점 이후의 createdTime
    public List<DagVersionEntity> findMySqlDescendantsCreatedAfter(Long startId, LocalDateTime threshold) {
        // MySQL native query 파라미터가 String이면
        // threshold.toString() or DateTimeFormatter 로 변환
        String thresholdStr = threshold.toString();
        return mysqlRepo.findDescendantsCreatedAfter(startId, thresholdStr);
    }

    public List<DagVersionNode> findNeoDescendantsCreatedAfter(String startId, LocalDateTime threshold) {
        return neoRepo.findDescendantsCreatedAfter(startId, threshold);
    }

    // 시나리오 C) 두 노드의 가장 최신 공통 조상
    public DagVersionEntity findMySqlLatestCommonAncestor(Long idA, Long idB) {
        return mysqlRepo.findLatestCommonAncestor(idA, idB);
    }

    public DagVersionNode findNeoLatestCommonAncestor(String idA, String idB) {
        return neoRepo.findLatestCommonAncestor(idA, idB);
    }

    // --- 시나리오 E ---
    public List<DagVersionEntity> findMySqlUpTo3StepsByAuthorTitle(Long startId, String author, String titlePart) {
        return mysqlRepo.findUpTo3StepsByAuthorTitle(startId, author, titlePart);
    }

    public List<DagVersionNode> findNeoUpTo3StepsByAuthorTitle(String startId, String author, String titlePart) {
        return neoRepo.findUpTo3StepsByAuthorTitle(startId, author, titlePart);
    }
}
