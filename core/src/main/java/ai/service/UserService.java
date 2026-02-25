package ai.service;

import ai.dto.own.request.UserCreateRequestDto;
import ai.dto.own.request.UserUpdateRequestDto;
import ai.dto.own.response.UserResponseDto;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.UserMapper;
import ai.repository.RoleRepository;
import ai.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponseDto getById(String id){
        return userMapper.entityToResponseDto(
                userRepository.findById(UUID.fromString(id))
                        .orElseThrow(() -> new AppException(ApiResponseStatus.USER_NOT_EXISTS)));
    }

    public List<UserResponseDto> getAll(){
        return userRepository.findAll().stream().map(userMapper::entityToResponseDto).toList();
    }

    public UserResponseDto create(UserCreateRequestDto createRequestDto){
        if(userRepository.existsByUserName(createRequestDto.getUserName()))
            throw new AppException(ApiResponseStatus.USER_EXISTED);
        UserEntity newEntity = userMapper.createRequestDtoToEntity(createRequestDto);

        newEntity.setPassword(passwordEncoder.encode(createRequestDto.getPassword()));

        List<RoleEntity> roles = roleRepository.findAllById(createRequestDto.getRoles());

        if(roles.size()!=createRequestDto.getRoles().size())
            throw new AppException(ApiResponseStatus.ROLE_NAME_NOT_EXISTS);

        newEntity.setRoles(new HashSet<>(roles));

        return userMapper.entityToResponseDto(userRepository.save(newEntity));
    }

    public UserResponseDto update(String id, UserUpdateRequestDto updateRequestDto){
        UserEntity entity = userRepository.findById(UUID.fromString(id)).orElseThrow(() -> new AppException(ApiResponseStatus.USER_NOT_EXISTS));
        userMapper.updateEntity(entity, updateRequestDto);

        List<RoleEntity> roles = roleRepository.findAllById(updateRequestDto.getRoles());

        if(roles.size()!=updateRequestDto.getRoles().size())
            throw new AppException(ApiResponseStatus.PERMISSION_NAME_NOT_EXISTS);

        entity.setRoles(new HashSet<>(roles));

        return userMapper.entityToResponseDto(userRepository.save(entity));
    }

    public void delete(String id){
        userRepository.deleteById(UUID.fromString(id));
    }
}
