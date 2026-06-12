package com.mts.online_shop.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * После миграций Liquibase синхронизирует users.xml с БД (добавляет учётки и BCrypt-хеши).
 * Запускается до {@link com.mts.online_shop.camunda.CamundaIdentitySynchronizer}.
 */
@Component
@Order(50)
public class UsersXmlBootstrap implements ApplicationRunner {

    private final XmlUserDetailsService xmlUserDetailsService;

    public UsersXmlBootstrap(XmlUserDetailsService xmlUserDetailsService) {
        this.xmlUserDetailsService = xmlUserDetailsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        xmlUserDetailsService.syncUsersXmlFromDatabase();
    }
}
