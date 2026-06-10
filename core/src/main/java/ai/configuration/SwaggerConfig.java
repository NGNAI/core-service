package ai.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                "Bearer token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .in(SecurityScheme.In.HEADER)
                                        .bearerFormat("JWT")
                                        .scheme("bearer")
                                        .name("bearerAuth")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer token"))
                .info(new Info().title("Backend core API").version("1.0"));
    }

    @Bean
    public OperationCustomizer customizePermissions() {
        return (operation, handlerMethod) -> {
            // Tìm xem method có gắn @PreAuthorize không
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize != null) {
                String expression = preAuthorize.value();
                
                // Format lại chuỗi expression đó thành dạng Markdown đẹp đẽ
                String permissionMarkdown = "\n\n--- \n**🔐 Required Security Expression:** `" + expression + "`";
                
                // Nối vào description hiện tại của API
                String currentDesc = operation.getDescription() != null ? operation.getDescription() : "";
                operation.setDescription(currentDesc + permissionMarkdown);
            }
            return operation;
        };
    }
}
