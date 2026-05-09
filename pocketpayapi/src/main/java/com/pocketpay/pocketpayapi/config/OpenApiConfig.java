package com.pocketpay.pocketpayapi.config;

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
    public OpenAPI pocketPayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PocketPay Wallet API")
                        .version("1.0.0")
                        .description("A Nigerian fintech wallet API built with Spring Boot. " +
                                "Features include JWT authentication, wallet management, " +
                                "internal transfers, transaction history, and Paystack integration " +
                                "for funding wallets.")
                        .contact(new Contact()
                                .name("Daniel")
                                .email("danniteme@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here. " +
                                                "Get one by calling /api/auth/login")));
    }
}