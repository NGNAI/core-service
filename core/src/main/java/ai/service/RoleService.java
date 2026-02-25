package ai.service;

import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.response.RoleResponseDto;
import ai.entity.postgres.PermissionEntity;
import ai.entity.postgres.RoleEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.RoleMapper;
import ai.repository.PermissionRepository;
import ai.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    public List<RoleResponseDto> getAll(){
        return roleRepository.findAll().stream().map(roleMapper::entityToResponseDto).toList();
    }

    public RoleResponseDto create(RoleCreateRequestDto createRequestDto){
        if(roleRepository.existsById(createRequestDto.getName()))
            throw new AppException(ApiResponseStatus.ROLE_NAME_EXISTED);
        RoleEntity newEntity = roleMapper.createRequestDtoToEntity(createRequestDto);

        List<PermissionEntity> permissions = permissionRepository.findAllById(createRequestDto.getPermissions());

        if(permissions.size()!=createRequestDto.getPermissions().size())
            throw new AppException(ApiResponseStatus.PERMISSION_NAME_NOT_EXISTS);

        newEntity.setPermissions(new HashSet<>(permissions));

        return roleMapper.entityToResponseDto(roleRepository.save(newEntity));
    }

    public RoleResponseDto update(String name, RoleUpdateRequestDto updateRequestDto){
        RoleEntity entity = roleRepository.findById(name).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_NAME_NOT_EXISTS));
        roleMapper.updateEntity(entity, updateRequestDto);

        List<PermissionEntity> permissions = permissionRepository.findAllById(updateRequestDto.getPermissions());

        if(permissions.size()!=updateRequestDto.getPermissions().size())
            throw new AppException(ApiResponseStatus.PERMISSION_NAME_NOT_EXISTS);

        entity.setPermissions(new HashSet<>(permissions));

        return roleMapper.entityToResponseDto(roleRepository.save(entity));
    }

    public void delete(String name){
        roleRepository.deleteById(name);
    }
}
