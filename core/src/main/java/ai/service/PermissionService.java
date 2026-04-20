package ai.service;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.request.filter.PermissionFilterDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.dto.own.response.PermissionWithRoleScopeResponseDto;
import ai.entity.postgres.PermissionEntity;
import ai.entity.postgres.RolePermissionEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.PermissionMapper;
import ai.model.CustomPairModel;
import ai.repository.PermissionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class  PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    public CustomPairModel<Long,List<PermissionResponseDto>> getAll(PermissionFilterDto filterDto){
        Page<PermissionEntity> page = permissionRepository.findAll(
                filterDto.createSpec(),
                filterDto.createPageable()
        );

        return new CustomPairModel<>(page.getTotalElements(),page.getContent().stream().map(permissionMapper::entityToResponseDto).toList());
    }

    public PermissionResponseDto create(PermissionCreateRequestDto createRequestDto){
        if(permissionRepository.existsByName(createRequestDto.getName()))
            throw new AppException(ApiResponseStatus.PERMISSION_NAME_EXISTED);
        PermissionEntity newEntity = permissionMapper.createRequestDtoToEntity(createRequestDto);

        return permissionMapper.entityToResponseDto(permissionRepository.save(newEntity));
    }

    public PermissionResponseDto update(UUID id, PermissionUpdateRequestDto updateRequestDto){
        PermissionEntity entity = permissionRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.PERMISSION_ID_NOT_EXISTS));
        permissionMapper.updateEntity(entity, updateRequestDto);

        return permissionMapper.entityToResponseDto(permissionRepository.save(entity));
    }

    public void delete(UUID id){
        permissionRepository.deleteById(id);
    }

    public Set<PermissionWithRoleScopeResponseDto> rolePermissionsToPermissionDto(Set<RolePermissionEntity> rpEntities) {
        if(rpEntities==null || rpEntities.isEmpty())
            return Set.of();
        return rpEntities.stream().map(rpEntity->{
            PermissionWithRoleScopeResponseDto responseWithScope = permissionMapper.entityToScopeResponseDto(rpEntity.getPermission());
            responseWithScope.setScope(rpEntity.getScope());

            return responseWithScope;
        }).collect(Collectors.toSet());
    }
}
