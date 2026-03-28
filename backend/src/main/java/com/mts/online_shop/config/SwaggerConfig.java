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

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = BASIC_AUTH;
        
        return new OpenAPI()
                .info(new Info()
                        .title("MTS Online Shop")
                        .version("0.1.0")
                        .description("""
                                ### Публичные API (без авторизации)
                                - `GET /api/products/**` - просмотр товаров
                                
                                ### API по ролям:
                                
                                **ADMIN (администратор)** - все API:
                                - `GET/POST/PUT/DELETE /api/users/**` - управление пользователями
                                - `GET/POST/PUT/DELETE /api/admin/products/**` - управление товарами
                                - `POST /api/transactions/orders/{id}/cancel` - отмена заказа с возвратом
                                - `GET/POST /api/orders/**` - все операции с заказами
                                - `GET/POST /api/cart/**` - все операции с корзиной
                                
                                **OPERATOR (оператор)**:
                                - `GET /api/products/**` - просмотр товаров
                                - `GET /api/orders/**` - просмотр заказов
                                - `GET /api/cart/**` - просмотр корзин
                                - `POST /api/cart/{id}/clear` - очистка корзины
                                - `POST /api/transactions/orders/{id}/cancel` - отмена заказа с возвратом
                                
                                **CUSTOMER (покупатель)**:
                                - `GET /api/products/**` - просмотр товаров
                                - `GET /api/orders` - свои заказы
                                - `POST /api/orders/create` - создание заказа
                                - `POST /api/orders/{id}/cancel` - отмена своего заказа
                                - `GET /api/cart` - своя корзина
                                - `POST /api/cart/items` - добавление в корзину
                                
                                Для авторизации введите логин и пароль из users.xml
                                (admin/password123, customer1/password123)
                                """
                        )
                )
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Введите логин и пароль из users.xml")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName));
    }
}
