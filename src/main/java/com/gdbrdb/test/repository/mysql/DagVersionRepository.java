package com.gdbrdb.test.repository.mysql;

import com.gdbrdb.test.entity.mysql.DagVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DagVersionRepository extends JpaRepository<DagVersionEntity, Long> {

    /**
     * 특정 노드(child)의 '조상' 중
     * author = :author
     * AND title LIKE %:titlePart%
     * 인 노드들을 반환
     */
    @Query(value = """
        WITH RECURSIVE ancestors AS (
          SELECT dvp.child_version_id, dvp.parent_version_id
          FROM dag_version_parents dvp
          WHERE dvp.child_version_id = :startId
          
          UNION ALL
          
          SELECT dvp.child_version_id, dvp.parent_version_id
          FROM dag_version_parents dvp
          INNER JOIN ancestors a ON dvp.child_version_id = a.parent_version_id
        )
        SELECT dv.*
        FROM dag_version dv
        JOIN ancestors a ON dv.id = a.parent_version_id
        WHERE dv.author = :author
          AND dv.title LIKE %:titlePart%
        """, nativeQuery = true)
    List<DagVersionEntity> findAncestorsByAuthorAndTitle(
            @Param("startId") Long startId,
            @Param("author") String author,
            @Param("titlePart") String titlePart
    );

    /**
     * 특정 노드의 '자손' 중
     * createdTime >= :timeThreshold 인 노드들을 반환
     */
    @Query(value = """
        WITH RECURSIVE descs AS (
          SELECT dvp.parent_version_id, dvp.child_version_id
          FROM dag_version_parents dvp
          WHERE dvp.parent_version_id = :startId
          
          UNION ALL
          
          SELECT dvp.parent_version_id, dvp.child_version_id
          FROM dag_version_parents dvp
          INNER JOIN descs d ON dvp.parent_version_id = d.child_version_id
        )
        SELECT dv.*
        FROM dag_version dv
        JOIN descs d ON dv.id = d.child_version_id
        WHERE dv.created_time >= :timeThreshold
        """, nativeQuery = true)
    List<DagVersionEntity> findDescendantsCreatedAfter(
            @Param("startId") Long startId,
            @Param("timeThreshold") String timeThreshold
    );

    /**
     * 두 노드(A,B)의 공통 조상 중
     * createdTime이 가장 늦은(최신) 노드 1개
     *
     * (예시는 단순히 "ORDER BY created_time DESC LIMIT 1" 로 처리)
     */
    @Query(value = """
        WITH RECURSIVE ancestorsA AS (
          SELECT dvp.child_version_id, dvp.parent_version_id
          FROM dag_version_parents dvp WHERE dvp.child_version_id = :idA
          UNION ALL
          SELECT dvp.child_version_id, dvp.parent_version_id
          FROM dag_version_parents dvp
          INNER JOIN ancestorsA a ON dvp.child_version_id = a.parent_version_id
        ),
        ancestorsB AS (
          SELECT dvp.child_version_id, dvp.parent_version_id
          FROM dag_version_parents dvp WHERE dvp.child_version_id = :idB
          UNION ALL
          SELECT dvp.child_version_id, dvp.parent_version_id
          FROM dag_version_parents dvp
          INNER JOIN ancestorsB b ON dvp.child_version_id = b.parent_version_id
        )
        SELECT dv.*
        FROM dag_version dv
        JOIN (
          SELECT DISTINCT a.parent_version_id AS pid
          FROM ancestorsA a
          JOIN ancestorsB b ON a.parent_version_id = b.parent_version_id
          WHERE a.parent_version_id IS NOT NULL
        ) c ON dv.id = c.pid
        ORDER BY dv.created_time DESC
        LIMIT 1
        """, nativeQuery = true)
    DagVersionEntity findLatestCommonAncestor(
            @Param("idA") Long idA,
            @Param("idB") Long idB
    );

    /**
     * 시나리오 E) (예) 특정 노드로부터 최대 3단계 (parents) 이내 노드 중
     * author='chulsu' AND title LIKE '%ppt%'인 노드를 찾는다.
     *  - Depth=3 이하 => 재귀 CTE에서 depth tracking
     */
    @Query(value = """
        WITH RECURSIVE step AS (
          SELECT :startId AS current_id, 0 AS depth
          UNION ALL
          SELECT p.parent_version_id, s.depth+1
          FROM dag_version_parents p
          JOIN step s ON p.child_version_id = s.current_id
          WHERE s.depth < 3
        )
        SELECT dv.*
        FROM dag_version dv
        JOIN step st ON dv.id = st.current_id
        WHERE dv.author = :author
          AND dv.title LIKE %:titlePart%
        """, nativeQuery = true)
    List<DagVersionEntity> findUpTo3StepsByAuthorTitle(
            @Param("startId") Long startId,
            @Param("author") String author,
            @Param("titlePart") String titlePart
    );
}
