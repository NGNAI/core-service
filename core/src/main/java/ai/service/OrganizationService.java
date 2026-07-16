package ai.service;

import java.util.*;
import java.util.stream.Collectors;

import ai.annotation.Audited;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.exception.AppException;
import ai.model.PermissionGrantModel;
import ai.util.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import ai.dto.own.request.OrganizationCreateRequestDto;
import ai.dto.own.request.OrganizationUpdateRequestDto;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.entity.postgres.OrganizationEntity;
import ai.enums.ApiResponseStatus;
import ai.mapper.OrganizationMapper;
import ai.model.CustomPairModel;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.ModelAttribute;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class OrganizationService {
    OrganizationRepository orgRepository;
    OrganizationUserRoleRepository ourRepository;

    OrganizationMapper orgMapper;

    OrganizationUserRoleService ourService;

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
        populateTotalUser(responseDto);
        return responseDto;
    }

    public CustomPairModel<Long,List<OrganizationResponseDto>> getAll(OrganizationFilterDto filterDto){
        Page<OrganizationEntity> page = orgRepository.findAll(filterDto.createSpec(),filterDto.createPageable());
        List<OrganizationResponseDto> organizations = page.getContent().stream().map(orgMapper::entityToResponseDto).toList();
        return new CustomPairModel<>(page.getTotalElements(),organizations);
    }

    public OrganizationEntity getRoot(){
        Specification<OrganizationEntity> spec = ((root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("parent")));
        return orgRepository.findOne(spec).orElse(null);
    }
    
    public OrganizationResponseDto getRoot(Integer nestedChild){
        OrganizationEntity orgRoot = getRoot();
        if(getRoot()==null)
            throw new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS);
        OrganizationResponseDto responseDto = orgMapper.entityToResponseDto(orgRoot);
        if(nestedChild!=null && nestedChild > 0)
            appendChild(1,nestedChild, responseDto);
        populateTotalUser(responseDto);
        return responseDto;
    }

    public CustomPairModel<Long,List<OrganizationResponseDto>> getByPermission(@ModelAttribute OrganizationFilterDto filterDto){
        List<PermissionGrantModel> permissions = ourService.getPermissionGrant(JwtUtil.getUserId(), JwtUtil.getOrgId());
        for(PermissionGrantModel permission : permissions) {
            if(!permission.getResource().equals(PermissionResource.ORG) || !permission.getAction().equals(PermissionAction.READ))
                continue;
            switch (permission.getScope()){
                case ALL -> {
                    return new CustomPairModel<>(1L,List.of(getRoot(999)));
                }
                case OWN -> {
                    return new CustomPairModel<>(1L,List.of(getById(JwtUtil.getOrgId(),0)));
                }
                case DESCENDANT -> {
                    return new CustomPairModel<>(1L,List.of(getById(JwtUtil.getOrgId(),999)));
                }
            }
        }

        throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
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

    @Audited(action = AuditAction.CREATE, resource = AuditResource.ORG, description = "Tạo tổ chức mới: {0}")
    public OrganizationResponseDto create(OrganizationCreateRequestDto requestDto){
        OrganizationEntity org = orgMapper.createRequestDtoToEntity(requestDto);

        if(requestDto.getParentId()!=null){
            OrganizationEntity orgParent = orgRepository.findById(requestDto.getParentId()).orElseThrow(() -> new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS));
            org.setParent(orgParent);
        }

        return orgMapper.entityToResponseDto(orgRepository.save(org));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.ORG, resourceIdExpression = "#arg0", description = "Cập nhật tổ chức: {0}")
    public OrganizationResponseDto update(UUID id, OrganizationUpdateRequestDto requestDto){
        OrganizationEntity org = orgRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        orgMapper.updateEntity(org,requestDto);
//        if(
//                (org.getParent()==null && requestDto.getParentId()!=null) ||
//                        (org.getParent()!=null && requestDto.getParentId()==null) ||
//                        (org.getParent()!=null && org.getParent().getId()!=requestDto.getParentId())
//        ){
//            if(requestDto.getParentId()!=null){
//                OrganizationEntity orgParent = orgRepository.findById(requestDto.getParentId()).orElseThrow(() -> new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS));
//                org.setParent(orgParent);
//            } else
//                org.setParent(null);
//        }

        return orgMapper.entityToResponseDto(orgRepository.save(org));
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.ORG, resourceIdExpression = "#arg0", description = "Xoá tổ chức: {0}")
    public void delete(UUID id) {
        if(orgRepository.countByParentId(id)>0)
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EMPTY);

        if(ourRepository.countUserRoleByOrgId(id)>0)
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EMPTY);

        orgRepository.deleteById(id);
    }

    public boolean isDescendant(UUID parentId, UUID childId){
        if(parentId==null || childId==null)
            return false;
        Map<UUID,String> map = orgRepository.findAllById(List.of(parentId,childId)).stream().collect(Collectors.toMap(OrganizationEntity::getId,OrganizationEntity::getPath));

        if(map.size()<2)
            return false;

        return map.get(childId).startsWith(map.get(parentId));
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

    /**
     * Populate totalUser for this node and all descendants in the tree.
     * totalUser = number of users directly assigned to this organization (not including descendants).
     */
    private void populateTotalUser(OrganizationResponseDto orgDto) {
        if (orgDto == null) return;

        Set<OrganizationResponseDto> toProcess = new HashSet<>();
        collectAllNodes(orgDto, toProcess);

        List<UUID> orgIds = toProcess.stream().map(OrganizationResponseDto::getId).toList();
        if (orgIds.isEmpty()) return;

        Map<UUID, Long> userCountByOrg = ourRepository.countUsersGroupByOrg(orgIds).stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        for (OrganizationResponseDto node : toProcess) {
            node.setTotalUser(userCountByOrg.getOrDefault(node.getId(), 0L));
        }
    }

    private void collectAllNodes(OrganizationResponseDto orgDto, Set<OrganizationResponseDto> result) {
        if (orgDto == null || result.contains(orgDto)) return;
        result.add(orgDto);
        if (orgDto.getChildren() != null) {
            for (OrganizationResponseDto child : orgDto.getChildren()) {
                collectAllNodes(child, result);
            }
        }
    }
}