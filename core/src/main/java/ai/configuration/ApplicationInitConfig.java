package ai.configuration;

import ai.dto.own.request.OrganizationAssignUserRequestDto;
import ai.dto.own.request.OrganizationCreateRequestDto;
import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.UserCreateRequestDto;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.dto.own.response.RoleResponseDto;
import ai.dto.own.response.UserResponseDto;
import ai.service.OrganizationService;
import ai.service.OrganizationUserRoleService;
import ai.service.RoleService;
import ai.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class ApplicationInitConfig {
    @Bean
    ApplicationRunner applicationRunner(OrganizationService organizationService, UserService userService, RoleService roleService, OrganizationUserRoleService ourService){
        log.info("Init application...");
        return args -> {
            if(organizationService.getRoot(null, new OrganizationFilterDto()).getFirst()==0){
                OrganizationResponseDto org = organizationService.create(OrganizationCreateRequestDto.builder()
                                .name("Root")
                                .description("Root organization")
                        .build());

                log.info("Init root organization");

                UserResponseDto user = userService.create(UserCreateRequestDto.builder()
                        .userName("admin")
                        .password("admin")
                        .firstName("Administrator")
                        .email("admin@tmp.com")
                        .source("local")
                        .build());

                log.info("Init admin/admin user");

                RoleResponseDto role = roleService.create(RoleCreateRequestDto.builder()
                        .name("ADMIN")
                        .description("Role for admin")
                        .defaultAssign(false)
                        .build());

                log.info("Init admin role");

                ourService.assignUsers(org.getId(), OrganizationAssignUserRequestDto.builder()
                                .userIds(Set.of(user.getId()))
                                .roleId(role.getId())
                        .build());
                log.info("Assign admin user to root organization with admin role");

                log.warn("Admin user has been created with default password: admin, please change it!");
            }
        };
    }
}
