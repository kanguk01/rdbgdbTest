package com.gdbrdb.test.entity.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionNodeBatchDTO {
    private String nodeId;
    private String content;
    private List<String> parentNodeIds;
}
