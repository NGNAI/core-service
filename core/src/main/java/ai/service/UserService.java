package ai.service;

import ai.dto.own.request.UserCreateRequestDto;
import ai.dto.own.request.UserUpdateRequestDto;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.UserResponseDto;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.UserMapper;
import ai.model.CustomPairModel;
import ai.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponseDto getById(UUID id){
        return userMapper.entityToResponseDto(getEntityById(id));
    }

    public UserEntity getEntityById(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiResponseStatus.USER_NOT_EXISTS));
    }

    public void validateUserId(UUID topicId){
        if(!userRepository.existsById(topicId))
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);
    }

    public CustomPairModel<Long,List<UserResponseDto>> getAll(UserFilterDto filterDto){
        Specification<UserEntity> spec = (root, query, criteriaBuilder) -> filterDto.createSpec(root,criteriaBuilder);
        Page<UserEntity> page = userRepository.findAll(spec,filterDto.createPageable());

        return new CustomPairModel<>(page.getTotalElements(),page.getContent().stream().map(userMapper::entityToResponseDto).toList());
    }

    public UserResponseDto create(UserCreateRequestDto createRequestDto){
        if(userRepository.existsByUserName(createRequestDto.getUserName()))
            throw new AppException(ApiResponseStatus.USER_EXISTED);
        UserEntity newEntity = userMapper.createRequestDtoToEntity(createRequestDto);

        newEntity.setPassword(passwordEncoder.encode(createRequestDto.getPassword()));

        return userMapper.entityToResponseDto(userRepository.save(newEntity));
    }

    public UserResponseDto update(UUID id, UserUpdateRequestDto updateRequestDto){
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.USER_NOT_EXISTS));
        userMapper.updateEntity(entity, updateRequestDto);

        return userMapper.entityToResponseDto(userRepository.save(entity));
    }

    public void delete(UUID id){
        userRepository.deleteById(id);
    }

    public UserEntity getRoot(){
        return userRepository.findByUserName("root").orElseThrow(() -> new AppException(ApiResponseStatus.ROOT_USER_NOT_EXIST));
    }
}
