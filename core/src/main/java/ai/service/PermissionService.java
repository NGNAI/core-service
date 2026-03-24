package ai.service;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.request.filter.PermissionFilterDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.entity.postgres.PermissionEntity;
import ai.entity.postgres.RolePermissionEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.PermissionMapper;
import ai.model.CustomPairModel;
import ai.repository.PermissionRepository;
import ai.util.StringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class PermissionService {
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
        newEntity.setName(StringUtil.toConstantCase(newEntity.getName()));

        return permissionMapper.entityToResponseDto(permissionRepository.save(newEntity));
    }

    public PermissionResponseDto update(UUID id, PermissionUpdateRequestDto updateRequestDto){
        PermissionEntity entity = permissionRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.PERMISSION_ID_NOT_EXISTS));
        permissionMapper.updateEntity(entity, updateRequestDto);
        entity.setName(StringUtil.toConstantCase(entity.getName()));

        return permissionMapper.entityToResponseDto(permissionRepository.save(entity));
    }

    public void delete(UUID id){
        permissionRepository.deleteById(id);
    }

    public Set<PermissionResponseDto> rolePermissionsToPermissionDto(Set<RolePermissionEntity> rpEntities) {
        if(rpEntities==null || rpEntities.isEmpty())
            return Set.of();
        return rpEntities.stream().map(rpEntity->permissionMapper.entityToResponseDto(rpEntity.getPermission())).collect(Collectors.toSet());
    }
}
