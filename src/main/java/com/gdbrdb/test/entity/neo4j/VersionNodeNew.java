package com.gdbrdb.test.entity.neo4j;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Node("VersionNew")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VersionNodeNew {

    // 사용자 정의 식별자: UUID 문자열
    @org.springframework.data.neo4j.core.schema.Id
    private String nodeId = UUID.randomUUID().toString();

    private String content;

    // 부모 관계 (새로운 라벨 데이터를 위한 관계도 동일)
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.OUTGOING)
    private List<VersionNodeNew> parents = new ArrayList<>();

    public void addParent(VersionNodeNew parent) {
        this.parents.add(parent);
    }
}
