package ai.service;

import ai.dto.own.request.*;
import ai.dto.own.request.filter.UserFilterDto;
import ai.dto.own.response.RoleSimplifyResponseDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.enums.PermissionScope;
import ai.exeption.AppException;
import ai.mapper.RoleMapper;
import ai.mapper.UserMapper;
import ai.model.CustomPairModel;
import ai.model.PermissionGrantModel;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import ai.repository.RoleRepository;
import ai.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OrganizationUserRoleService {
    RoleService roleService;
    UserRepository userRepository;
    RoleRepository roleRepository;
    OrganizationRepository orgRepository;
    OrganizationUserRoleRepository ourRepository;
    UserMapper userMapper;
    RoleMapper roleMapper;

//    @Cacheable(cacheNames = CacheName.USER_PERMISSION_IN_ORG, key = "#userId + ':' + #orgId")
    public List<PermissionGrantModel> getPermissionGrant(UUID userId, UUID orgId) {
        System.out.println("Getting permission grant");
        Map<UUID, Map<String, Map<String, Map<String, String>>>> mapRolePermission = roleService.getPermissionListOfRole();

        return ourRepository.findByUserAndOrgWithPermission(userId, orgId).stream()
            .flatMap(ourEntity -> {
                UUID roleId = ourEntity.getId().getRoleId();
                Map<String, Map<String, Map<String, String>>> rolePerm = mapRolePermission.get(roleId);
                if (rolePerm == null)
                    throw new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS);

                return rolePerm.entrySet().stream() // resource
                    .flatMap(resourceEntry -> {
                        String resource = resourceEntry.getKey();
                        return resourceEntry.getValue().entrySet().stream() // action
                            .flatMap(actionEntry -> {
                                String action = actionEntry.getKey();
                                return actionEntry.getValue().entrySet().stream() // target
                                    .map(targetEntry -> {
                                        String target = targetEntry.getKey();
                                        String scope = targetEntry.getValue();
                                        return PermissionGrantModel.builder()
                                            .resource(PermissionResource.valueOf(resource))
                                            .action(PermissionAction.valueOf(action))
                                            .targetResource("_self".equals(target)
                                                    ? null
                                                    : PermissionResource.valueOf(target))
                                            .scope(PermissionScope.valueOf(scope))
                                            .orgId(orgId)
                                            .build();
                                    });
                            });
                    });
            })
            .toList();
    }

    @PreAuthorize("@perm.canAccess(#orgId, 'ORG', 'READ',null)")
    public CustomPairModel<Long, List<UserWithRoleInOrgResponseDto>> getUsersByOrgId(UUID orgId, UserFilterDto userFilterDto) {
        if (!orgRepository.existsById(orgId)) throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);

        Map<UUID, UserWithRoleInOrgResponseDto> mapResult = new HashMap<>();

        Specification<OrganizationUserRoleEntity> spec = (root, query, criteriaBuilder) -> {
            Join<?, ?> user = root.join("user");

            Predicate userSearch = userFilterDto.createSpec(user, criteriaBuilder);
            Predicate orgIdSearch = criteriaBuilder.equal(root.get("organization").get("id"), orgId);
            return criteriaBuilder.and(userSearch, orgIdSearch);
        };

        Page<OrganizationUserRoleEntity> users = ourRepository.findAll(spec, userFilterDto.createPageable());
        if (!users.isEmpty()) {
            Map<UUID, Map<String, Map<String, Map<String, String>>>> mapRolePermission = roleService.getPermissionListOfRole();

            users.forEach(our -> {
                UUID userId = our.getUser().getId();
                RoleSimplifyResponseDto role = roleMapper.entityToSimplifyResponseDto(our.getRole());
                role.setPermissions(mapRolePermission.getOrDefault(role.getId(), Map.of()));
                if (!mapResult.containsKey(userId)) {
                    UserWithRoleInOrgResponseDto userResponseDto = userMapper.entityToWithRoleResponseDto(our.getUser());
                    userResponseDto.getRoles().add(role);

                    mapResult.put(userId, userResponseDto);
                } else mapResult.get(userId).getRoles().add(role);
            });
        }

        return new CustomPairModel<>(users.getTotalElements(), mapResult.values().stream().toList());
    }

    @PreAuthorize("@perm.canAccess(#orgId, 'ORG', 'READ',null)")
    public CustomPairModel<Long, List<UserResponseDto>> getUsersNotInOrg(UUID orgId, UserFilterDto userFilterDto) {
        if (!orgRepository.existsById(orgId)) throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);
        Specification<UserEntity> spec = (root, query, criteriaBuilder) -> {
            Predicate userSearch = userFilterDto.createSpec(root, criteriaBuilder);
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<OrganizationUserRoleEntity> our = sub.from(OrganizationUserRoleEntity.class);
            sub.select(criteriaBuilder.literal(1));

            sub.where(criteriaBuilder.equal(our.get("organization").get("id"), orgId), criteriaBuilder.equal(our.get("user"), root));

            Predicate notExists = criteriaBuilder.not(criteriaBuilder.exists(sub));

            return criteriaBuilder.and(userSearch, notExists);
        };

        Page<UserEntity> page = userRepository.findAll(spec, userFilterDto.createPageable());

        return new CustomPairModel<>(page.getTotalElements(), page.stream().map(userMapper::entityToResponseDto).toList());
    }

    @PreAuthorize("@perm.canAccess(#id, 'ORG', 'ASSIGN', 'USER')")
    public void assignUsers(UUID id, OrganizationAssignUserRequestDto requestDto) {
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if (users.size() < requestDto.getUserIds().size()) throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        ourRepository.findUserRoleByOrgId(id).forEach(our -> {
            if (requestDto.getUserIds().contains(our.getId().getUserId()))
                throw new AppException(ApiResponseStatus.USER_EXISTED_IN_ORGANIZATION);
        });

        RoleEntity role;

        if (requestDto.getRoleId() != null) {
            role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));
        } else {
            role = roleRepository.findByDefaultAssign().orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_DEFAULT_ASSIGN_NOT_EXISTS));
        }

        ourRepository.saveAll(users.stream().map(user -> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }

    @PreAuthorize("@perm.canAccess(#id, 'ORG', 'ASSIGN', 'ROLE')")
    public void assignRole(UUID id, OrganizationAssignRoleRequestDto requestDto) {
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if (users.size() < requestDto.getUserIds().size()) throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(id, requestDto.getUserIds());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if (distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        RoleEntity role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        ourRepository.findByOrganizationIdAndRoleId(id, requestDto.getRoleId()).forEach(our -> {
            if (requestDto.getUserIds().contains(our.getId().getUserId()))
                throw new AppException(ApiResponseStatus.USER_WITH_ROLE_EXISTED_IN_ORGANIZATION);
        });

        ourRepository.saveAll(users.stream().map(user -> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }

    @PreAuthorize("@perm.canAccess(#orgId, 'ORG', 'REMOVE', 'USER')")
    public void removeUsers(UUID orgId, OrganizationRemoveUserRequestDto requestDto) {
        if (!orgRepository.existsById(orgId)) throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());
        if (users.size() < requestDto.getUserIds().size()) throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(orgId, requestDto.getUserIds());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if (distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        ourRepository.deleteAll(ourList);
    }

    @PreAuthorize("@perm.canAccess(#orgId, 'ORG', 'REMOVE', 'ROLE')")
    public void removeRole(UUID orgId, OrganizationRemoveRoleRequestDto requestDto) {
        OrganizationEntity org = orgRepository.findById(orgId).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if (users.size() < requestDto.getUserIds().size()) throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        RoleEntity role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(orgId, requestDto.getUserIds());

        long singleRoleCount = ourList.stream().collect(Collectors.groupingBy(our -> our.getUser().getId())).values().stream().filter(list -> list.size() == 1).mapToLong(List::size).sum();

        if (singleRoleCount > 0)
            throw new AppException(ApiResponseStatus.USER_MUST_HAVE_AT_LEAST_ONE_ROLE_IN_ORGANIZATION);

        ourRepository.deleteAll(users.stream().map(user -> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }

    @PreAuthorize("@perm.canAccess(#orgId, 'ORG', 'ASSIGN', 'ROLE')")
    public void replaceRole(UUID orgId, OrganizationReplaceRoleRequestDto requestDto) {
        OrganizationEntity org = orgRepository.findById(orgId).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if (users.size() < requestDto.getUserIds().size()) throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        if (!roleRepository.existsById(requestDto.getOldRoleId()))
            throw new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS);

        RoleEntity newRole = roleRepository.findById(requestDto.getNewRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndRoleId(orgId, requestDto.getOldRoleId());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if (distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_WITH_ROLE_NOT_EXIST_IN_ORGANIZATION);

        ourRepository.deleteAll(ourList);

        ourRepository.findByOrganizationIdAndRoleId(orgId, requestDto.getNewRoleId()).forEach(our -> {
            if (requestDto.getUserIds().contains(our.getId().getUserId()))
                throw new AppException(ApiResponseStatus.USER_WITH_ROLE_EXISTED_IN_ORGANIZATION);
        });

        ourRepository.saveAll(users.stream().map(user -> new OrganizationUserRoleEntity(org, user, newRole)).collect(Collectors.toSet()));
    }

    @PreAuthorize("@perm.canAccess(#orgId, 'ORG', 'ASSIGN', 'ROLE')")
    public void resetRole(UUID orgId, OrganizationResetRoleRequestDto requestDto) {
        OrganizationEntity org = orgRepository.findById(orgId).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        List<UserEntity> users = userRepository.findAllById(requestDto.getUserIds());

        if (users.size() < requestDto.getUserIds().size()) throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);

        List<OrganizationUserRoleEntity> ourList = ourRepository.findByOrganizationIdAndUserIdIn(orgId, requestDto.getUserIds());

        int distinctUserId = ourList.stream().map(our -> our.getId().getUserId()).collect(Collectors.toSet()).size();

        if (distinctUserId < requestDto.getUserIds().size())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        RoleEntity role = roleRepository.findById(requestDto.getRoleId()).orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));

        ourRepository.deleteAll(ourList);
        ourRepository.saveAll(users.stream().map(user -> new OrganizationUserRoleEntity(org, user, role)).collect(Collectors.toSet()));
    }
}
