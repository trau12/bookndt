package com.ndt.gateway.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Rest data management API",
                version = "1.0.0"
        ),
        servers = {
                @Server(url = "http://localhost:8888", description = "API Gateway"),
                @Server(url = "http://localhost:8081/identity", description = "Identity Service"),
                @Server(url = "http://localhost:8082/profile", description = "Profile Service"),
                @Server(url = "http://localhost:8083/notification", description = "Notification Service")
        },
        security = @SecurityRequirement(name = "BearerAuth")
)

@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Enter token"

)
@Configuration
public class SwaggerConfig {
        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                        .route("identity_service", r -> r.path("/identity/**")
                                .uri("http://localhost:8081"))
                        .route("profile_service", r -> r.path("/profile/**")
                                .uri("http://localhost:8082"))
                        .route("notification_service", r -> r.path("/notification/**")
                                .uri("http://localhost:8083"))
                        .build();
        }

        @Bean
        public GroupedOpenApi identityApi() {
                return GroupedOpenApi.builder()
                        .group("identity-api")
                        .displayName("Identity API")
                        .pathsToMatch("/identity/**")
                        .build();
        }

        @Bean
        public GroupedOpenApi profileApi() {
                return GroupedOpenApi.builder()
                        .group("profile-api")
                        .displayName("Profile API")
                        .pathsToMatch("/profile/**")
                        .build();
        }

        @Bean
        public GroupedOpenApi notificationApi() {
                return GroupedOpenApi.builder()
                        .group("notification-api")
                        .displayName("Notification API")
                        .pathsToMatch("/notification/**")
                        .build();
        }
}
