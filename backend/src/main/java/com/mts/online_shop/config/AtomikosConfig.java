package com.mts.online_shop.config;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class AtomikosConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String databaseDriverClassName;

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean dataSource() {
        AtomikosDataSourceBean dataSource = new AtomikosDataSourceBean();
        dataSource.setUniqueResourceName("postgresqlDataSource");
        dataSource.setXaDataSourceClassName(databaseDriverClassName);
        
        Properties xaProperties = new Properties();
        xaProperties.setProperty("url", databaseUrl);
        xaProperties.setProperty("user", databaseUsername);
        xaProperties.setProperty("password", databasePassword);
        
        dataSource.setXaProperties(xaProperties);
        dataSource.setPoolSize(10);
        dataSource.setBorrowConnectionTimeout(30);
        dataSource.setMaxIdleTime(60);
        dataSource.setMaxPoolSize(20);
        dataSource.setMinPoolSize(5);
        
        return dataSource;
    }

    @Bean
    @DependsOn("dataSource")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
