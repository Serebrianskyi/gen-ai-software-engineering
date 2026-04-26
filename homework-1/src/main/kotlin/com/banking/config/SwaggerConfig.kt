package com.banking.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Banking Transactions API")
                    .version("1.0.0")
                    .description("Simple REST API for banking transactions management")
                    .contact(
                        Contact()
                            .name("Banking API Support")
                            .url("https://banking.local")
                            .email("support@banking.local")
                    )
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                    )
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server")
            )
            .addServersItem(
                Server()
                    .url("https://api.banking.local")
                    .description("Production Server")
            )
    }
}