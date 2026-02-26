package ai.service;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.entity.postgres.PermissionEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.PermissionMapper;
import ai.repository.PermissionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    public List<PermissionResponseDto> getAll(){
        return permissionRepository.findAll().stream().map(permissionMapper::entityToResponseDto).toList();
    }

    public PermissionResponseDto create(PermissionCreateRequestDto createRequestDto){
        if(permissionRepository.existsByName(createRequestDto.getName()))
            throw new AppException(ApiResponseStatus.PERMISSION_NAME_EXISTED);
        PermissionEntity newEntity = permissionMapper.createRequestDtoToEntity(createRequestDto);

        return permissionMapper.entityToResponseDto(permissionRepository.save(newEntity));
    }

    public PermissionResponseDto update(int id, PermissionUpdateRequestDto updateRequestDto){
        PermissionEntity entity = permissionRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.PERMISSION_ID_NOT_EXISTS));
        permissionMapper.updateEntity(entity, updateRequestDto);

        return permissionMapper.entityToResponseDto(permissionRepository.save(entity));
    }

    public void delete(int id){
        permissionRepository.deleteById(id);
    }
}
