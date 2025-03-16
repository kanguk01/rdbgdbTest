package com.gdbrdb.test.entity.neo4j;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Node("DagVersion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DagVersionNode {

    @Id
    private String nodeId;

    private String title;
    private String content;
    private String author;

    @Property("createdTime")
    private OffsetDateTime createdTime;

    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.INCOMING)
    private List<DagVersionNode> parents = new ArrayList<>();

    public void addParent(DagVersionNode parent) {
        if (!this.parents.contains(parent)) {
            this.parents.add(parent);
        }
    }
}
