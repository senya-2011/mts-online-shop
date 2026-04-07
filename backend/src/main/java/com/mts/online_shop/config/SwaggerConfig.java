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

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = JWT_AUTH;
        
        return new OpenAPI()
                .info(new Info()
                        .title("MTS Online Shop")
                        .version("0.1.0")
                        .description("""
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
                                """
                        )
                )
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Введите JWT токен")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName));
    }
}
