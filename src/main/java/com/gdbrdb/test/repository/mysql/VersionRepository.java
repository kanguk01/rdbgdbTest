package com.gdbrdb.test.repository.mysql;

import com.gdbrdb.test.entity.mysql.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VersionRepository extends JpaRepository<VersionEntity, Long> {

    /**
     * 특정 버전의 '모든 조상(ancestor) ID'를 찾는 재귀 CTE 예시.
     */
    @Query(value = """
        WITH RECURSIVE ancestors AS (
          SELECT vp.child_version_id, vp.parent_version_id
          FROM version_parents vp
          WHERE vp.child_version_id = :startId
          
          UNION ALL
          
          SELECT vp.child_version_id, vp.parent_version_id
          FROM version_parents vp
          INNER JOIN ancestors a ON vp.child_version_id = a.parent_version_id
        )
        SELECT parent_version_id
        FROM ancestors
        WHERE parent_version_id IS NOT NULL
        """, nativeQuery = true)
    List<Long> findAllAncestorIds(@Param("startId") Long startId);

    /**
     * 특정 버전의 '모든 자식(children) ID'를 찾는 재귀 CTE 예시.
     */
    @Query(value = """
        WITH RECURSIVE descendants AS (
          SELECT vp.parent_version_id, vp.child_version_id
          FROM version_parents vp
          WHERE vp.parent_version_id = :startId
          
          UNION ALL
          
          SELECT vp.parent_version_id, vp.child_version_id
          FROM version_parents vp
          INNER JOIN descendants d ON vp.parent_version_id = d.child_version_id
        )
        SELECT child_version_id
        FROM descendants
        WHERE child_version_id IS NOT NULL
        """, nativeQuery = true)
    List<Long> findAllDescendantIds(@Param("startId") Long startId);

    /**
     * 두 버전 간 '공통 조상 후보 ID'들을 찾는 예시.
     */
    @Query(value = """
        WITH RECURSIVE ancestorsA AS (
          SELECT vp.child_version_id, vp.parent_version_id
          FROM version_parents vp WHERE vp.child_version_id = :idA
          UNION ALL
          SELECT vp.child_version_id, vp.parent_version_id
          FROM version_parents vp
          INNER JOIN ancestorsA a ON vp.child_version_id = a.parent_version_id
        ),
        ancestorsB AS (
          SELECT vp.child_version_id, vp.parent_version_id
          FROM version_parents vp WHERE vp.child_version_id = :idB
          UNION ALL
          SELECT vp.child_version_id, vp.parent_version_id
          FROM version_parents vp
          INNER JOIN ancestorsB b ON vp.child_version_id = b.parent_version_id
        )
        SELECT DISTINCT a.parent_version_id
        FROM ancestorsA a
        JOIN ancestorsB b ON a.parent_version_id = b.parent_version_id
        WHERE a.parent_version_id IS NOT NULL
        """, nativeQuery = true)
    List<Long> findCommonAncestorIds(@Param("idA") Long idA, @Param("idB") Long idB);

    /**
     * EXPLAIN 실행 계획: 단순 SELECT 쿼리에 대한 실행 계획을 조회.
     * (실제 재귀 CTE에 대한 EXPLAIN은 MySQL 버전과 설정에 따라 다르게 보일 수 있음)
     */
    @Query(value = "EXPLAIN SELECT * FROM versions WHERE id = :id", nativeQuery = true)
    List<Object[]> explainSelectById(@Param("id") Long id);
}