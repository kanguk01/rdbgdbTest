package com.gdbrdb.test.entity.mysql;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 다중부모 DAG 실험용 엔티티
 */
@Entity
@Table(name = "dag_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DagVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private String author;
    private LocalDateTime createdTime;

    @ManyToMany
    @JoinTable(
            name = "dag_version_parents",
            joinColumns = @JoinColumn(name = "child_version_id"),
            inverseJoinColumns = @JoinColumn(name = "parent_version_id")
    )
    private List<DagVersionEntity> parents = new ArrayList<>();

    public void addParent(DagVersionEntity parent) {
        if (!this.parents.contains(parent)) {
            this.parents.add(parent);
        }
    }
}
