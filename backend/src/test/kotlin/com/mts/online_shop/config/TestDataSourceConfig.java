package com.mts.online_shop.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseBuilder;
import org.springframework.boot.jdbc.EmbeddedDatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import javax.sql.DataSource;

@TestConfiguration
public class TestDataSourceConfig {

    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .addScript("schema.sql")
                .build();
    }
}
