package com.gdbrdb.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootTest
@EnableJpaRepositories(
		basePackages = "com.gdbrdb.test.repository.mysql",
		transactionManagerRef = "transactionManager"
)
@EnableNeo4jRepositories(
		basePackages = "com.gdbrdb.test.repository.neo4j",
		transactionManagerRef = "neo4jTransactionManager"
)
class TestApplicationTests {

	@Test
	void contextLoads() {
	}

}
