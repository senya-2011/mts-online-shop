package com.mts.online_shop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String JWT_AUTH = "jwtAuth";
    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MTS Online Shop")
                        .version("0.1.0")
                        .description("""
                                ### Вход в Swagger (отдельно от Camunda Tasklist UI)
                                1. **basicAuth** — логин/пароль из `users.xml` (после деплоя те же учётки синхронизируются в Camunda).
                                2. **jwtAuth** — вызовите `POST /api/auth/login`, скопируйте `accessToken`, вставьте в Authorize (без префикса Bearer).
                                
                                Учётки по умолчанию: `admin` / `admin`, `user` / `user` (если не меняли `users.xml`).
                                
                                ### Публичные API 
                                - `GET /api/products/**` - просмотр товаров
                                - `POST /api/auth/login` - аутентификация и получение JWT токена
                                - `POST /api/auth/register` - регистрация нового пользователя
                                ### API по ролям:
                                **ADMIN (администратор)** - все API:
                                - `GET/POST/PUT/DELETE /api/admin/users/**` - управление пользователями
                                - `GET/POST/PUT/DELETE /api/admin/products/**` - управление товарами
                                - `POST /api/admin/orders/{id}/cancel` - отмена заказа с возвратом
                                - `GET/POST /api/admin/orders/**` - все операции с заказами
                                - `GET/POST /api/admin/cart/**` - все операции с корзиной
                                
                                **USER (пользователь)**:
                                - `GET /api/products/**` - просмотр товаров
                                - `GET /api/orders` - свои заказы
                                - `POST /api/orders/{id}/create` - создание заказа с оплатой
                                - `POST /api/orders/{id}/cancel` - отмена своего заказа
                                - `GET /api/cart` - своя корзина
                                - `POST /api/cart/items` - добавление в корзину
                                
                                **Demo (без JWT):**
                                - `POST /api/demo/telegram/broadcast` - рассылка «Распродажа» в RabbitMQ
                                - `GET /api/demo/telegram/broadcast-page` - HTML-кнопка для рассылки
                                """
                        )
                )
                .components(new Components()
                        .addSecuritySchemes(JWT_AUTH,
                                new SecurityScheme()
                                        .name(JWT_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT из POST /api/auth/login (только значение токена)"))
                        .addSecuritySchemes(BASIC_AUTH,
                                new SecurityScheme()
                                        .name(BASIC_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Логин и пароль из users.xml")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
