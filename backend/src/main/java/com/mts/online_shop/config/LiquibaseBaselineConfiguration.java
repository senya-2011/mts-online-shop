package com.mts.online_shop.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Если схема приложения уже создана (например, после предыдущего запуска),
 * но таблица databasechangelog пуста — помечаем changeset'ы как выполненные (changelogSync),
 * чтобы не падать на CREATE TABLE ... already exists.
 */
@Configuration
@EnableConfigurationProperties(LiquibaseProperties.class)
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = true)
public class LiquibaseBaselineConfiguration {

    @Bean
    @Primary
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties properties) {
        BaselineSpringLiquibase liquibase = new BaselineSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setContexts(properties.getContexts());
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setLiquibaseSchema(properties.getLiquibaseSchema());
        liquibase.setLiquibaseTablespace(properties.getLiquibaseTablespace());
        liquibase.setDatabaseChangeLogTable(properties.getDatabaseChangeLogTable());
        liquibase.setDatabaseChangeLogLockTable(properties.getDatabaseChangeLogLockTable());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(properties.isEnabled());
        liquibase.setChangeLogParameters(properties.getParameters());
        liquibase.setRollbackFile(properties.getRollbackFile());
        liquibase.setTestRollbackOnUpdate(properties.isTestRollbackOnUpdate());
        liquibase.setLabelFilter(properties.getLabelFilter());
        return liquibase;
    }

    static class BaselineSpringLiquibase extends SpringLiquibase {

        @Override
        public void afterPropertiesSet() throws LiquibaseException {
            baselineExistingSchemaIfNeeded();
            super.afterPropertiesSet();
        }

        private void baselineExistingSchemaIfNeeded() throws LiquibaseException {
            try (Connection connection = getDataSource().getConnection()) {
                if (tableExists(connection, "users") && isLiquibaseHistoryEmpty(connection)) {
                    try (JdbcConnection jdbcConnection = new JdbcConnection(connection)) {
                        Database database = DatabaseFactory.getInstance()
                                .findCorrectDatabaseImplementation(jdbcConnection);
                        String changeLogPath = resolveClasspathChangeLog(getChangeLog());
                        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                        if (classLoader.getResource(changeLogPath) == null) {
                            classLoader = getClass().getClassLoader();
                        }
                        Liquibase liquibase = new Liquibase(
                                changeLogPath,
                                new ClassLoaderResourceAccessor(classLoader),
                                database
                        );
                        liquibase.changeLogSync((String) null);
                    }
                }
            } catch (SQLException e) {
                throw new LiquibaseException("Failed to baseline existing database schema", e);
            }
        }

        private boolean tableExists(Connection connection, String tableName) throws SQLException {
            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();
            try (ResultSet rs = meta.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = meta.getTables(catalog, schema, tableName.toLowerCase(), new String[]{"TABLE"})) {
                return rs.next();
            }
        }

        private String resolveClasspathChangeLog(String changeLog) {
            if (changeLog == null) {
                return null;
            }
            if (changeLog.startsWith("classpath:")) {
                changeLog = changeLog.substring("classpath:".length());
            }
            if (changeLog.startsWith("/")) {
                changeLog = changeLog.substring(1);
            }
            return changeLog;
        }

        private boolean isLiquibaseHistoryEmpty(Connection connection) throws SQLException {
            if (!tableExists(connection, "databasechangelog")) {
                return true;
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM databasechangelog")) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
            return true;
        }
    }
}
