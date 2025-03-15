package com.gdbrdb.test.entity.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * VersionNodeNew
 * - nodeId, content 필드
 * - 부모 리스트를 INCOMING 관계로 맵핑
 *   parent --(:PARENT_OF)--> child
 *   child 는 @Relationship(direction=INCOMING)으로 부모를 참조
 */
@Node("VersionNew")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"parents"}) // 직렬화 시 무한 루프 방지 (선택)
public class VersionNodeNew {

    @Id
    private String nodeId;

    private String content;

    /**
     * "PARENT_OF" 관계가
     * parent (OUTGOING) -> child 이므로,
     * child 입장에서는 INCOMING
     */
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.INCOMING)
    private List<VersionNodeNew> parents = new ArrayList<>();

    public void addParent(VersionNodeNew parent) {
        this.parents.add(parent);
    }

    public VersionNodeNew(String nodeId, String content) {
        this.nodeId = nodeId;
        this.content = content;
    }
}
