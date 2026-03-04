package ai.service;

import ai.dto.own.request.*;
import ai.dto.own.response.OrganizationResponseDto;
import ai.dto.own.response.RoleResponseDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.OrganizationMapper;
import ai.mapper.RoleMapper;
import ai.mapper.UserMapper;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import ai.repository.RoleRepository;
import ai.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OrganizationService {
    OrganizationRepository orgRepository;
    UserRepository userRepository;
    RoleRepository roleRepository;
    OrganizationUserRoleRepository ourRepository;

    OrganizationMapper orgMapper;
    UserMapper userMapper;
    RoleMapper roleMapper;

    public OrganizationResponseDto getById(int id, Integer nestedChild){
        OrganizationResponseDto responseDto = orgMapper.entityToResponseDto(
                orgRepository.findById(id)
                        .orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS)));
        if(nestedChild!=null && nestedChild > 0)
            appendChild(1,nestedChild, responseDto);
        else
            responseDto.setChildren(null);
        return responseDto;
    }

    public List<OrganizationResponseDto> getAll(){
        return orgRepository.findAll().stream().map(orgMapper::entityToResponseDto).toList();
    }

    public List<OrganizationResponseDto> getRoot(Integer nestedChild){
        return orgRepository.findByParentIsNull().stream().map(entity -> {
            OrganizationResponseDto childResponseDto = orgMapper.entityToResponseDto(entity);
            if(nestedChild!=null && nestedChild > 0)
                appendChild(1,nestedChild, childResponseDto);

            return childResponseDto;
        }).collect(Collectors.toList());
    }

    public List<OrganizationResponseDto> getChild(int parentId, Integer nestedChild){
        if(!orgRepository.existsById(parentId))
            throw new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS);
        return orgRepository.findByParentId(parentId).stream().map(entity -> {
            OrganizationResponseDto childResponseDto = orgMapper.entityToResponseDto(entity);
            if(nestedChild!=null && nestedChild > 0)
                appendChild(1,nestedChild, childResponseDto);

            return childResponseDto;
        }).collect(Collectors.toList());
    }

    public OrganizationResponseDto create(OrganizationCreateRequestDto requestDto){
        OrganizationEntity org = orgMapper.createRequestDtoToEntity(requestDto);

        if(requestDto.getParentId()!=null && requestDto.getParentId()>0){
            OrganizationEntity orgParent = orgRepository.findById(requestDto.getParentId()).orElseThrow(() -> new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS));
            org.setParent(orgParent);
        }
        return orgMapper.entityToResponseDto(orgRepository.save(org));
    }

    public OrganizationResponseDto update(int id, OrganizationUpdateRequestDto requestDto){
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        orgMapper.updateEntity(org,requestDto);
        if(
                (org.getParent()==null && requestDto.getParentId()!=null) ||
                        (org.getParent()!=null && requestDto.getParentId()==null) ||
                        (org.getParent()!=null && org.getParent().getId()!=requestDto.getParentId())
        ){
            if(requestDto.getParentId()!=null){
                OrganizationEntity orgParent = orgRepository.findById(requestDto.getParentId()).orElseThrow(() -> new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS));
                org.setParent(orgParent);
            } else
                org.setParent(null);
        }

        return orgMapper.entityToResponseDto(orgRepository.save(org));
    }

    public void delete(int id) {
        if(orgRepository.countByParentId(id)>0)
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EMPTY);

        if(ourRepository.countUserRoleByOrgId(id)>0)
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EMPTY);

        orgRepository.deleteById(id);
    }

    public List<UserWithRoleInOrgResponseDto> getUsersByOrgId(int orgId){
        if(!orgRepository.existsById(orgId))
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);

        Map<Integer, UserWithRoleInOrgResponseDto> mapResult = new HashMap<>();
        ourRepository.findUserRoleByOrgId(orgId).forEach(our->{
            int userId = our.getUser().getId();
            RoleResponseDto role = roleMapper.entityToResponseDto(our.getRole());
            if(!mapResult.containsKey(userId)) {
                UserWithRoleInOrgResponseDto userResponseDto = userMapper.entityToWithRoleResponseDto(our.getUser());
                userResponseDto.getRoles().add(role);

                mapResult.put(userId, userResponseDto);
            } else
                mapResult.get(userId).getRoles().add(role);
        });
        return mapResult.values().stream().toList();
    }

    public List<UserResponseDto> getUsersNotInOrg(int orgId){
        if(!orgRepository.existsById(orgId))
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);

        return ourRepository.findUsersNotInOrg(orgId).stream().map(userMapper::entityToResponseDto).toList();
    }

    public void assignUsers(int id, OrganizationAssignUserRequestDto requestDto){
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if(users.size() < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        ourRepository.findUserRoleByOrgId(id).forEach(our->{
            if(requestDto.getUserIds().contains(our.getId().getUserId()))
                throw new AppException(ApiResponseStatus.USER_EXISTED_IN_ORGANIZATION);
        });

        RoleEntity role;

        if(requestDto.getRoleId()!=null){
            role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));
        } else {
            role = roleRepository.findByDefaultAssign().orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_DEFAULT_ASSIGN_NOT_EXISTS));
        }

        ourRepository.saveAll(users.stream().map(user-> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }

    public void assignRole(int id, OrganizationAssignRoleRequestDto requestDto){
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if(users.size() < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(id,requestDto.getUserIds());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if(distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        RoleEntity role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        ourRepository.findByOrganizationIdAndRoleId(id, requestDto.getRoleId()).forEach(our->{
            if(requestDto.getUserIds().contains(our.getId().getUserId()))
                throw new AppException(ApiResponseStatus.USER_WITH_ROLE_EXISTED_IN_ORGANIZATION);
        });

        ourRepository.saveAll(users.stream().map(user-> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }

    public void removeUsers(int id, OrganizationRemoveUserRequestDto requestDto){
        if(!orgRepository.existsById(id))
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());
        if(users.size() < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(id,requestDto.getUserIds());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if(distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        ourRepository.deleteAll(ourList);
    }

    public void removeRole(int id, OrganizationRemoveRoleRequestDto requestDto){
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if(users.size() < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        RoleEntity role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(id, requestDto.getUserIds());

        long singleRoleCount = ourList.stream().collect(Collectors.groupingBy(our->our.getUser().getId()))
                .values().stream().filter(list -> list.size()==1)
                .mapToLong(List::size)
                .sum();

        if(singleRoleCount>0)
            throw new AppException(ApiResponseStatus.USER_MUST_HAVE_AT_LEAST_ONE_ROLE_IN_ORGANIZATION);

        ourRepository.deleteAll(users.stream().map(user-> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }

    public void replaceRole(int orgId, OrganizationReplaceRoleRequestDto requestDto){
        OrganizationEntity org = orgRepository.findById(orgId).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if(users.size() < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        if(!roleRepository.existsById(requestDto.getOldRoleId()))
            throw new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS);

        RoleEntity newRole = roleRepository.findById(requestDto.getNewRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndRoleId(orgId, requestDto.getOldRoleId());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if(distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_WITH_ROLE_NOT_EXIST_IN_ORGANIZATION);

        ourRepository.deleteAll(ourList);

        ourRepository.findByOrganizationIdAndRoleId(orgId, requestDto.getNewRoleId()).forEach(our->{
            if(requestDto.getUserIds().contains(our.getId().getUserId()))
                throw new AppException(ApiResponseStatus.USER_WITH_ROLE_EXISTED_IN_ORGANIZATION);
        });

        ourRepository.saveAll(users.stream().map(user-> new OrganizationUserRoleEntity(org, user, newRole)).collect(Collectors.toSet()));
    }

    public void resetRole(int id, OrganizationResetRoleRequestDto requestDto) {
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if(users.size() < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(id,requestDto.getUserIds());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if(distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        RoleEntity role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        ourRepository.deleteAll(ourList);
        ourRepository.saveAll(users.stream().map(user-> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }

    private void appendChild(int currentNestedLevel, int nestedLevel, OrganizationResponseDto parentOrg){
        if(nestedLevel==0 || currentNestedLevel > nestedLevel)
            return;

        List<OrganizationEntity> listChild = orgRepository.findByParentId(parentOrg.getId());
        parentOrg.setChildren(new HashSet<>());
        for(OrganizationEntity child : listChild) {
            OrganizationResponseDto childResponseDto = orgMapper.entityToResponseDto(child);
            parentOrg.getChildren().add(childResponseDto);
            if (currentNestedLevel < nestedLevel) {
                appendChild(currentNestedLevel+1,nestedLevel,childResponseDto);
            }
        }
    }
}
