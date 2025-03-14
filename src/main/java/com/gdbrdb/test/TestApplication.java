package com.gdbrdb.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication(
		exclude = {
				org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration.class,
				org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
		}
)
@EntityScan("com.gdbrdb.test.entity")  // MySQL 엔티티들이 있는 패키지
@EnableJpaRepositories(
		basePackages = "com.gdbrdb.test.repository.mysql",
		transactionManagerRef = "transactionManager"
)
@EnableNeo4jRepositories(
		basePackages = "com.gdbrdb.test.repository.neo4j",
		transactionManagerRef = "neo4jTransactionManager"
)
public class TestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}

}
