package com.mts.online_shop.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import javax.sql.DataSource;

@TestConfiguration
public class TestJtaConfig {

    @Bean
    @Primary
    public PlatformTransactionManager testTransactionManager(DataSource dataSource) {
        // Используем стандартный Spring transaction manager вместо JTA
        return new DataSourceTransactionManager(dataSource);
    }
}
