package com.gdbrdb.test.entity.neo4j;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * nodeId는 이제 사용자가 직접 넣을 수도 있게 하고,
 * 디폴트 UUID.randomUUID()는 제거
 */
@Node("VersionNew")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class VersionNodeNew {

    @Id
    private String nodeId; // 더 이상 = UUID.randomUUID().toString() 없음

    private String content;

    // 부모 관계
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.OUTGOING)
    private List<VersionNodeNew> parents = new ArrayList<>();

    // 부모 관계 추가
    public void addParent(VersionNodeNew parent) {
        this.parents.add(parent);
    }

    // 편의 생성자 (nodeId, content 동시에 넣어주기)
    public VersionNodeNew(String nodeId, String content) {
        this.nodeId = nodeId;
        this.content = content;
    }
}
