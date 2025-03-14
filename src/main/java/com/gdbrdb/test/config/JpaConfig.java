package com.gdbrdb.test.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class JpaConfig {

    // JPA가 자동으로 EntityManagerFactory를 만듦(만약 auto-config를 일부 쓰면).
    // 혹은 @Bean으로 LocalContainerEntityManagerFactoryBean 을 직접 정의할 수도 있음.

    // 만약 transactionManagerRef="transactionManager" 라고 했으면,
    // 아래 메서드 이름 또는 @Bean(name="transactionManager") 로 맞춰 줘야 함.
    @Bean(name = "transactionManager")
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}