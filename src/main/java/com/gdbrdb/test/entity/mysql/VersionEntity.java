package com.gdbrdb.test.entity.mysql;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 다중 부모(머지)를 지원하기 위해 @ManyToMany 사용.
 * -> version_parents 라는 중간 테이블이 생성됨
 *    (child_version_id, parent_version_id) 컬럼으로 매핑.
 */
@Entity
@Table(name = "versions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;  // 버전 내용(혹은 스냅샷 정보)

    @ManyToMany
    @JoinTable(
            name = "version_parents",
            joinColumns = @JoinColumn(name = "child_version_id"),
            inverseJoinColumns = @JoinColumn(name = "parent_version_id")
    )
    private List<VersionEntity> parents = new ArrayList<>();

    public void addParent(VersionEntity parent) {
        this.parents.add(parent);
    }
}