package ai.service;

import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.RolePermissionUpdateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.response.RoleResponseDto;
import ai.entity.postgres.PermissionEntity;
import ai.entity.postgres.RoleEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.PermissionMapper;
import ai.mapper.RoleMapper;
import ai.repository.PermissionRepository;
import ai.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RoleService {
    PermissionService permissionService;

    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;
    
    public RoleResponseDto getById(int roleId){
        RoleEntity role = roleRepository.findByIdWithPermissions(roleId).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        RoleResponseDto responseDto = roleMapper.entityToResponseDto(role);
        responseDto.setPermissions(permissionService.rolePermissionsToPermissionDto(role.getRolePermissions()));

        return responseDto;
    }

    public List<RoleResponseDto> getAll(){
        return roleRepository.findAllWithPermissions().stream().map(roleEntity -> {
            RoleResponseDto responseDto = roleMapper.entityToResponseDto(roleEntity);
            responseDto.setPermissions(permissionService.rolePermissionsToPermissionDto(roleEntity.getRolePermissions()));
            return responseDto;
        }).toList();
    }

    public RoleResponseDto create(RoleCreateRequestDto createRequestDto){
        if(roleRepository.existsByName(createRequestDto.getName()))
            throw new AppException(ApiResponseStatus.ROLE_NAME_EXISTED);
        RoleEntity newEntity = roleMapper.createRequestDtoToEntity(createRequestDto);

        if(newEntity.isDefaultAssign())
            roleRepository.deActiveAllDefaultAssign();

        return roleMapper.entityToResponseDto(roleRepository.save(newEntity));
    }

    public RoleResponseDto update(int id, RoleUpdateRequestDto updateRequestDto){
        RoleEntity entity = roleRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));
        roleMapper.updateEntity(entity, updateRequestDto);

        if(entity.isDefaultAssign())
            roleRepository.deActiveAllDefaultAssign();

        return roleMapper.entityToResponseDto(roleRepository.save(entity));
    }

    public RoleResponseDto assignPermissions(int roleId, RolePermissionUpdateRequestDto requestDto){
        List<PermissionEntity> permissions = permissionRepository.findAllById(requestDto.getPermissionIds());
        RoleEntity roleEntity = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        if(permissions.size() < requestDto.getPermissionIds().size())
            throw new AppException(ApiResponseStatus.PERMISSION_ID_NOT_EXISTS);

        roleEntity.getRolePermissions().clear();

        permissions.forEach(roleEntity::addPermission);

        RoleResponseDto responseDto = roleMapper.entityToResponseDto(roleRepository.save(roleEntity));
        responseDto.setPermissions(permissionService.rolePermissionsToPermissionDto(roleEntity.getRolePermissions()));
        return responseDto;
    }

    public void delete(int id){
        roleRepository.deleteById(id);
    }
}
