package com.mts.online_shop.config;

import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

@Configuration
public class NarayanaDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Bean(name = "xaDataSource")
    public PGXADataSource xaDataSource() {
        PGXADataSource xaDataSource = new PGXADataSource();
        xaDataSource.setUrl(databaseUrl);
        xaDataSource.setUser(databaseUsername);
        xaDataSource.setPassword(databasePassword);
        return xaDataSource;
    }

    @Bean(name = "dataSource")
    @DependsOn("xaDataSource")
    public DataSource dataSource(TransactionManager transactionManager) {
        PGXADataSource xaDataSource = xaDataSource();
        
        // Narayana XADataSource wrapper
        return new com.arjuna.ats.jdbc.TransactionalDataSource(xaDataSource, transactionManager);
    }

    @Bean
    @DependsOn("dataSource")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
