package ai.configuration;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

import ai.dto.own.response.BadRequestResponseDto;
import ai.dto.own.response.ForbiddenResponseDto;
import ai.dto.own.response.UnauthorizedResponseDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {
        
    @Bean
    public OpenAPI openAPI() {
        // Đổi từ "Bearer token" thành "bearerAuth" (viết liền, không dấu cách)
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .in(SecurityScheme.In.HEADER)
                                        .bearerFormat("JWT")
                                        .scheme("bearer")
                                        .name("bearerAuth")
                        )
                        .addSchemas("BadRequestResponseDto", ModelConverters.getInstance().read(BadRequestResponseDto.class).get("BadRequestResponseDto"))
                        .addSchemas("UnauthorizedResponseDto", ModelConverters.getInstance().read(UnauthorizedResponseDto.class).get("UnauthorizedResponseDto"))
                        .addSchemas("ForbiddenResponseDto", ModelConverters.getInstance().read(ForbiddenResponseDto.class).get("ForbiddenResponseDto"))
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .info(new Info().title("Backend core API").version("1.0"));
    }

    // 1. Nhóm API dành cho User
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("User")
                .pathsToMatch("/user/**")
                .build();
    }

    // 2. Nhóm API dành cho Admin
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("Admin")
                .pathsToMatch("/admin/**")
                .build();
    }

    // 3. Nhóm API chung (loại trừ user và admin)
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("General")          // Tên nhóm hiển thị trên Swagger
                .pathsToMatch("/**")        // Khớp với mọi API trong hệ thống
                .pathsToExclude("/user/**", "/admin/**") // LOẠI TRỪ 2 nhóm trên
                .build();
    }

    @Bean
    public OperationCustomizer globalResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            // Thêm response 400 nếu chưa có
            if (!responses.containsKey("400")) {
                responses.addApiResponse("400", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Bad Request")
                        .content(new Content()
                                .addMediaType("application/json",
                                        new MediaType()
                                                .schema(new Schema<>()
                                                        .$ref("#/components/schemas/BadRequestResponseDto")))));
            }

            // Thêm response 401 nếu chưa có
            if (!responses.containsKey("401")) {
                responses.addApiResponse("401", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Unauthorized")
                        .content(new Content()
                                .addMediaType("application/json",
                                        new MediaType()
                                                .schema(new Schema<>()
                                                        .$ref("#/components/schemas/UnauthorizedResponseDto")))));
            }

            // Thêm response 403 nếu chưa có
            if (!responses.containsKey("403")) {
                responses.addApiResponse("403", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Forbidden")
                        .content(new Content()
                                .addMediaType("application/json",
                                        new MediaType()
                                                .schema(new Schema<>()
                                                        .$ref("#/components/schemas/ForbiddenResponseDto")))));
            }

            return operation;
        };
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
