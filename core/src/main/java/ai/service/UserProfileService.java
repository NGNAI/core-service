package ai.service;

import ai.dto.own.request.UserPasswordUpdateRequestDto;
import ai.dto.own.request.UserProfileUpdateRequestDto;
import ai.dto.own.response.OrganizationWithUserRoleDto;
import ai.dto.own.response.RoleSimplifyResponseDto;
import ai.dto.own.response.UserProfileResponseDto;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.UserSourceAction;
import ai.exeption.AppException;
import ai.mapper.RoleMapper;
import ai.mapper.UserMapper;
import ai.repository.OrganizationUserRoleRepository;
import ai.repository.UserRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class UserProfileService {
    RoleService roleService;
    OrganizationService organizationService;
    OrganizationUserRoleRepository ourRepository;

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    RoleMapper roleMapper;

    public UserProfileResponseDto getProfile(){
        UUID orgId = JwtUtil.getOrgId();
        UUID userId = JwtUtil.getUserId();
        UserEntity entity = userRepository.findById(JwtUtil.getUserId()).orElseThrow(() -> new AppException(ApiResponseStatus.USER_NOT_EXISTS));
        organizationService.validateOrgId(orgId);
        List<OrganizationUserRoleEntity> ours = ourRepository.findByUserAndOrgWithPermission(userId, orgId);

        if(ours.isEmpty())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        OrganizationWithUserRoleDto organizationWithUserRoleDto = new OrganizationWithUserRoleDto();
        OrganizationEntity organizationEntity = ours.getFirst().getOrganization();
        organizationWithUserRoleDto.setName(organizationEntity.getName());
        organizationWithUserRoleDto.setDescription(organizationEntity.getDescription());

        Map<UUID, Map<String, Map<String, Map<String, String>>>> mapRolePermission = roleService.getPermissionListOfRole();
        ours.forEach(our->{
            RoleEntity roleEntity = our.getRole();
            RoleSimplifyResponseDto role = roleMapper.entityToSimplifyResponseDto(roleEntity);
            role.setPermissions(mapRolePermission.getOrDefault(role.getId(),Map.of()));

            organizationWithUserRoleDto.getRoles().add(role);
        });

        UserProfileResponseDto userProfileResponseDto = userMapper.entityToProfileResponseDto(entity);
        userProfileResponseDto.setOrganization(organizationWithUserRoleDto);

        return userProfileResponseDto;
    }

    public UserProfileResponseDto update(UserProfileUpdateRequestDto updateRequestDto){
        UserEntity entity = userRepository.findById(JwtUtil.getUserId()).orElseThrow(() -> new AppException(ApiResponseStatus.USER_NOT_EXISTS));
        if(!entity.getSource().toUpperCase().equals(UserSourceAction.LOCAL.toString()))
            throw new AppException(ApiResponseStatus.ONLY_LOCAL_USER_CAN_UPDATE_INFO);
        userMapper.updateEntity(entity, updateRequestDto);

        return userMapper.entityToProfileResponseDto(userRepository.save(entity));
    }

    public void changePassword(UserPasswordUpdateRequestDto requestDto){
        UserEntity entity = userRepository.findById(JwtUtil.getUserId()).orElseThrow(() -> new AppException(ApiResponseStatus.USER_NOT_EXISTS));
        if(!entity.getSource().toUpperCase().equals(UserSourceAction.LOCAL.toString()))
            throw new AppException(ApiResponseStatus.ONLY_LOCAL_USER_CAN_UPDATE_INFO);

        if(!passwordEncoder.matches(requestDto.getOldPassword(),entity.getPassword()))
            throw new AppException(ApiResponseStatus.USER_PASSWORD_INCORRECT);

        entity.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(entity);
    }
}
