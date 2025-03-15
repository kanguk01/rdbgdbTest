package com.gdbrdb.test.repository.mysql;

import com.gdbrdb.test.entity.mysql.VersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VersionRepository extends JpaRepository<VersionEntity, Long> {

    /**
     * [수정안] 특정 버전의 '모든 조상(ancestor) VersionEntity'를 찾는 재귀 CTE.
     *  - 기존에는 parent_version_id만 반환했지만, 이제 versions 테이블과 JOIN 해서
     *    VersionEntity 전체를 반환.
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
        SELECT v.* 
        FROM versions v
        INNER JOIN ancestors a ON v.id = a.parent_version_id
        WHERE a.parent_version_id IS NOT NULL
        """, nativeQuery = true)
    List<VersionEntity> findAllAncestorEntities(@Param("startId") Long startId);

    /**
     * [수정안] 특정 버전의 '모든 자손(descendant) VersionEntity'를 찾는 재귀 CTE.
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
        SELECT v.* 
        FROM versions v
        INNER JOIN descendants d ON v.id = d.child_version_id
        WHERE d.child_version_id IS NOT NULL
        """, nativeQuery = true)
    List<VersionEntity> findAllDescendantEntities(@Param("startId") Long startId);

    /**
     * 두 버전 간 '공통 조상 후보' (ID만 뽑는 예시는 참고 용도)
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
     * EXPLAIN 실행 계획
     */
    @Query(value = "EXPLAIN SELECT * FROM versions WHERE id = :id", nativeQuery = true)
    List<Object[]> explainSelectById(@Param("id") Long id);

    /*
       기존의 findAllAncestorIds, findAllDescendantIds 등
       ID만 반환하는 메서드들도 필요하면 둬도 되고,
       테스트 목적에 따라 제거해도 됨.
     */
}
