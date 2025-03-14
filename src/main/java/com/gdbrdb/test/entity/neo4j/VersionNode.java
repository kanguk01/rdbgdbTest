package com.gdbrdb.test.entity.neo4j;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Node("Version")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VersionNode {

    /**
     * Neo4j 내부 ID가 아닌, **사용자 정의** 프로퍼티를 식별자로 사용.
     * 여기서는 UUID 문자열을 기본값으로 할당.
     */
    @Id
    private String nodeId = UUID.randomUUID().toString();

    // 예: Long id; 대신, string 형식 nodeId를 사용.
    //    (원하면 @GeneratedValue(strategy=...) 등 다른 방식 가능)

    private String content;

    // 부모들을 가리키는 관계 (방향성은 예시에 따라 조정)
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.OUTGOING)
    private List<VersionNode> parents = new ArrayList<>();

    public void addParent(VersionNode parent) {
        this.parents.add(parent);
    }
}