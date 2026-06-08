package ai.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import ai.annotation.Audited;
import ai.dto.own.request.PermissionAssignRequestDto;
import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.RolePermissionUpdateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.request.filter.RoleFilterDto;
import ai.dto.own.response.PermissionWithRoleScopeResponseDto;
import ai.dto.own.response.RoleResponseDto;
import ai.entity.postgres.PermissionEntity;
import ai.entity.postgres.RoleEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.exeption.AppException;
import ai.mapper.RoleMapper;
import ai.model.CustomPairModel;
import ai.repository.PermissionRepository;
import ai.repository.RoleRepository;
import ai.util.StringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RoleService {
    PermissionService permissionService;

    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    public RoleResponseDto getById(UUID roleId){
        RoleEntity role = roleRepository.findByIdWithPermissions(roleId).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        RoleResponseDto responseDto = roleMapper.entityToResponseDto(role);
        responseDto.setPermissions(permissionService.rolePermissionsToPermissionDto(role.getRolePermissions()));

        return responseDto;
    }

    public CustomPairModel<Long,List<RoleResponseDto>> getAll(RoleFilterDto filterDto){
        Page<RoleEntity> page = roleRepository.findAll(
                filterDto.createSpec(),
                filterDto.createPageable()
        );

        List<RoleResponseDto> roles = page.getContent().stream().map(roleEntity -> {
            RoleResponseDto responseDto = roleMapper.entityToResponseDto(roleEntity);
            responseDto.setPermissions(permissionService.rolePermissionsToPermissionDto(roleEntity.getRolePermissions()));
            return responseDto;
        }).toList();

        return new CustomPairModel<>(page.getTotalElements(),roles);
    }

    @Audited(action = AuditAction.CREATE, resource = AuditResource.ROLE, description = "Tạo vai trò: {0}")
    public RoleResponseDto create(RoleCreateRequestDto createRequestDto){
        if(roleRepository.existsByName(createRequestDto.getName()))
            throw new AppException(ApiResponseStatus.ROLE_NAME_EXISTED);
        RoleEntity newEntity = roleMapper.createRequestDtoToEntity(createRequestDto);

        if(createRequestDto.isDefaultAssign())
            roleRepository.deActiveAllDefaultAssign();
        newEntity.setName(StringUtil.toConstantCase(newEntity.getName()));

        return roleMapper.entityToResponseDto(roleRepository.save(newEntity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.ROLE, resourceIdExpression = "#arg0", description = "Cập nhật vai trò: {0}")
    public RoleResponseDto update(UUID id, RoleUpdateRequestDto updateRequestDto){
        if(updateRequestDto.isDefaultAssign())
            roleRepository.deActiveAllDefaultAssign();

        RoleEntity entity = roleRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));
        roleMapper.updateEntity(entity, updateRequestDto);
        entity.setName(StringUtil.toConstantCase(entity.getName()));

        return roleMapper.entityToResponseDto(roleRepository.save(entity));
    }

    @Audited(action = AuditAction.ASSIGN, resource = AuditResource.ROLE, resourceIdExpression = "#arg0", description = "Phân quyền cho vai trò: {0}")
    public RoleResponseDto assignPermissions(UUID roleId, RolePermissionUpdateRequestDto requestDto){
        List<PermissionEntity> permissions = permissionRepository.findAllById(requestDto.getPermissions().stream().map(PermissionAssignRequestDto::getId).collect(Collectors.toSet()));
        RoleEntity roleEntity = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        if(permissions.size() < requestDto.getPermissions().size())
            throw new AppException(ApiResponseStatus.PERMISSION_ID_NOT_EXISTS);

        roleEntity.getRolePermissions().clear();

        Map<UUID, String> mapPermissionScope = requestDto.getPermissions().stream().collect(Collectors.toMap(PermissionAssignRequestDto::getId,PermissionAssignRequestDto::getScope));

        permissions.forEach(permission->{roleEntity.addPermission(permission,mapPermissionScope.get(permission.getId()));});

        RoleResponseDto responseDto = roleMapper.entityToResponseDto(roleRepository.save(roleEntity));
        responseDto.setPermissions(permissionService.rolePermissionsToPermissionDto(roleEntity.getRolePermissions()));
        return responseDto;
    }

//    @Cacheable(cacheNames = CacheName.ROLE_AND_PERMISSION)
    public Map<UUID, Map<String, Map<String, Map<String, String>>>> getPermissionListOfRole() {
        //System.out.println("getting permission of role");
        RoleFilterDto roleFilter = new RoleFilterDto();
        roleFilter.setPageSize(50);

        return getAll(roleFilter).getSecond().stream()
                .collect(Collectors.toMap(
                        RoleResponseDto::getId,
                        role -> role.getPermissions().stream()
                                .collect(Collectors.groupingBy(
                                        PermissionWithRoleScopeResponseDto::getResource,
                                        Collectors.groupingBy(
                                                PermissionWithRoleScopeResponseDto::getAction,
                                                Collectors.toMap(
                                                        p -> p.getTargetResource() == null ? "_self" : p.getTargetResource(),
                                                        PermissionWithRoleScopeResponseDto::getScope,
                                                        (oldVal, newVal) -> newVal
                                                )
                                        )
                                ))
                ));
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.ROLE, resourceIdExpression = "#arg0", description = "Xoá vai trò: {0}")
    public void delete(UUID id){
        roleRepository.deleteById(id);
    }
}
