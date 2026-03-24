package ai.service;

import ai.dto.own.request.*;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.request.filter.RoleFilterDto;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.*;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.OrganizationMapper;
import ai.mapper.RoleMapper;
import ai.mapper.UserMapper;
import ai.model.CustomPairModel;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import ai.repository.RoleRepository;
import ai.repository.UserRepository;
import ai.util.LTreeUtil;
import jakarta.persistence.criteria.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OrganizationService {
    RoleService roleService;

    OrganizationRepository orgRepository;
    UserRepository userRepository;
    RoleRepository roleRepository;
    OrganizationUserRoleRepository ourRepository;

    OrganizationMapper orgMapper;
    UserMapper userMapper;
    RoleMapper roleMapper;

    public OrganizationEntity getEntityById(UUID id){
        return orgRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));
    }

    public void validateOrgId(UUID orgId){
        if(!orgRepository.existsById(orgId))
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);
    }

    public OrganizationResponseDto getById(UUID id, Integer nestedChild){
        OrganizationResponseDto responseDto = orgMapper.entityToResponseDto(
                orgRepository.findById(id)
                        .orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS)));
        if(nestedChild!=null && nestedChild > 0)
            appendChild(1,nestedChild, responseDto);
        else
            responseDto.setChildren(null);
        return responseDto;
    }

    public CustomPairModel<Long,List<OrganizationResponseDto>> getAll(OrganizationFilterDto filterDto){
        Page<OrganizationEntity> page = orgRepository.findAll(filterDto.createSpec(),filterDto.createPageable());
        List<OrganizationResponseDto> organizations = page.getContent().stream().map(orgMapper::entityToResponseDto).toList();
        return new CustomPairModel<>(page.getTotalElements(),organizations);
    }

    public CustomPairModel<Long,List<OrganizationResponseDto>> getRoot(Integer nestedChild, OrganizationFilterDto filterDto){
        Specification<OrganizationEntity> spec = filterDto.createSpec().and(((root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("parent"))));
        Page<OrganizationEntity> page = orgRepository.findAll(spec,filterDto.createPageable());
        List<OrganizationResponseDto> organizations = page.getContent().stream().map(entity -> {
            OrganizationResponseDto childResponseDto = orgMapper.entityToResponseDto(entity);
            if(nestedChild!=null && nestedChild > 0)
                appendChild(1,nestedChild, childResponseDto);

            return childResponseDto;
        }).toList();

        return new CustomPairModel<>(page.getTotalElements(),organizations);
    }

    public CustomPairModel<Long,List<OrganizationResponseDto>> getChild(UUID parentId, Integer nestedChild, OrganizationFilterDto filterDto){
        if(!orgRepository.existsById(parentId))
            throw new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS);
        Specification<OrganizationEntity> spec = filterDto.createSpec().and(((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("parent").get("id"),parentId)));

        Page<OrganizationEntity> page = orgRepository.findAll(spec,filterDto.createPageable());
        List<OrganizationResponseDto> organizations = page.getContent().stream().map(entity -> {
            OrganizationResponseDto childResponseDto = orgMapper.entityToResponseDto(entity);
            if(nestedChild!=null && nestedChild > 0)
                appendChild(1,nestedChild, childResponseDto);

            return childResponseDto;
        }).collect(Collectors.toList());

        return new CustomPairModel<>(page.getTotalElements(),organizations);
    }

    public OrganizationResponseDto create(OrganizationCreateRequestDto requestDto){
        UUID orgId = UUID.randomUUID();
        OrganizationEntity org = orgMapper.createRequestDtoToEntity(requestDto);
        org.setId(orgId);

        String parentPath = null;
        if(requestDto.getParentId()!=null){
            OrganizationEntity orgParent = orgRepository.findById(requestDto.getParentId()).orElseThrow(() -> new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS));
            org.setParent(orgParent);
            parentPath = orgParent.getPath();
        }
        org.setPath(LTreeUtil.buildPath(parentPath,orgId));

        return orgMapper.entityToResponseDto(orgRepository.save(org));
    }

    public OrganizationResponseDto update(UUID id, OrganizationUpdateRequestDto requestDto){
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

    public void delete(UUID id) {
        if(orgRepository.countByParentId(id)>0)
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EMPTY);

        if(ourRepository.countUserRoleByOrgId(id)>0)
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EMPTY);

        orgRepository.deleteById(id);
    }

    public CustomPairModel<Long,List<UserWithRoleInOrgResponseDto>> getUsersByOrgId(UUID orgId, UserFilterDto userFilterDto){
        if(!orgRepository.existsById(orgId))
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);

        Map<UUID, UserWithRoleInOrgResponseDto> mapResult = new HashMap<>();

        Specification<OrganizationUserRoleEntity> spec = (root, query, criteriaBuilder) -> {
            Join<?,?> user = root.join("user");

            Predicate userSearch = userFilterDto.createSpec(user, criteriaBuilder);
            Predicate orgIdSearch = criteriaBuilder.equal(root.get("organization").get("id"), orgId);
            return criteriaBuilder.and(userSearch,orgIdSearch);
        };

        Page<OrganizationUserRoleEntity> users = ourRepository.findAll(spec,userFilterDto.createPageable());
        if(!users.isEmpty()) {
            RoleFilterDto roleFilter = new RoleFilterDto();
            roleFilter.setPageSize(20);

            Map<UUID,Set<String>> mapRolePermission = roleService.getPermissionListOfRole(roleFilter);

            users.forEach(our->{
                UUID userId = our.getUser().getId();
                RoleSimplifyResponseDto role = roleMapper.entityToSimplifyResponseDto(our.getRole());
                role.setPermissions(mapRolePermission.getOrDefault(role.getId(),Set.of()));
                if(!mapResult.containsKey(userId)) {
                    UserWithRoleInOrgResponseDto userResponseDto = userMapper.entityToWithRoleResponseDto(our.getUser());
                    userResponseDto.getRoles().add(role);

                    mapResult.put(userId, userResponseDto);
                } else
                    mapResult.get(userId).getRoles().add(role);
            });
        }

        return new CustomPairModel<>(users.getTotalElements(),mapResult.values().stream().toList());
    }

    public CustomPairModel<Long,List<UserResponseDto>> getUsersNotInOrg(UUID orgId, UserFilterDto userFilterDto){
        if(!orgRepository.existsById(orgId))
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);
        Specification<UserEntity> spec = (root, query, criteriaBuilder) -> {
            Predicate userSearch = userFilterDto.createSpec(root, criteriaBuilder);
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<OrganizationUserRoleEntity> our = sub.from(OrganizationUserRoleEntity.class);
            sub.select(criteriaBuilder.literal(1));

            sub.where(
                    criteriaBuilder.equal(our.get("organization").get("id"), orgId),
                    criteriaBuilder.equal(our.get("user"), root));

            Predicate notExists = criteriaBuilder.not(criteriaBuilder.exists(sub));

            return criteriaBuilder.and(userSearch,notExists);
        };

        Page<UserEntity> page = userRepository.findAll(spec,userFilterDto.createPageable());

        return new CustomPairModel<>(page.getTotalElements(), page.stream().map(userMapper::entityToResponseDto).toList());
    }

    public void assignUsers(UUID id, OrganizationAssignUserRequestDto requestDto){
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

    public void assignRole(UUID id, OrganizationAssignRoleRequestDto requestDto){
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

    public void removeUsers(UUID id, OrganizationRemoveUserRequestDto requestDto){
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

    public void removeRole(UUID id, OrganizationRemoveRoleRequestDto requestDto){
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

    public void replaceRole(UUID orgId, OrganizationReplaceRoleRequestDto requestDto){
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

    public void resetRole(UUID id, OrganizationResetRoleRequestDto requestDto) {
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
