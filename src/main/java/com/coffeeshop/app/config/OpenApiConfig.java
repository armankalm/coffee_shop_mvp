package com.coffeeshop.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coffee Shop System API")
                        .description("REST API для системы управления кофейней. " +
                                "Поддерживает аутентификацию по email+OTP, заказы с кастомизацией, " +
                                "оплату через Kaspi и Stripe, а также управление меню и кофейнями.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Coffee Shop Team")
                                .email("support@coffeeshop.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Введите JWT токен, полученный при аутентификации")));
    }
}
