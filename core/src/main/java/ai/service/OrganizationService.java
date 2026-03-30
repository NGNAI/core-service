package ai.service;

import ai.dto.own.request.*;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.response.*;
import ai.entity.postgres.OrganizationEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.OrganizationMapper;
import ai.model.CustomPairModel;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import jakarta.persistence.criteria.*;
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
public class OrganizationService {
    OrganizationRepository orgRepository;
    OrganizationUserRoleRepository ourRepository;

    OrganizationMapper orgMapper;

    public OrganizationEntity getEntityById(UUID id){
        return orgRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));
    }

    public void validateOrgId(UUID orgId){
        if(!orgRepository.existsById(orgId))
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);
    }

    @PreAuthorize("@perm.canAccess(#id, 'ORG', 'READ',null)")
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

    @PreAuthorize("@perm.canAccess(null, 'ORG', 'READ',null)")
    public CustomPairModel<Long,List<OrganizationResponseDto>> getAll(OrganizationFilterDto filterDto){
        Page<OrganizationEntity> page = orgRepository.findAll(filterDto.createSpec(),filterDto.createPageable());
        List<OrganizationResponseDto> organizations = page.getContent().stream().map(orgMapper::entityToResponseDto).toList();
        return new CustomPairModel<>(page.getTotalElements(),organizations);
    }

//    @PreAuthorize("@perm.canAccess(null, 'ORG', 'READ',null)")
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

    @PreAuthorize("@perm.canAccess(#parentId, 'ORG', 'READ',null)")
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

    @PreAuthorize("@perm.canAccess(#requestDto.parentId, 'ORG', 'CREATE',null)")
    public OrganizationResponseDto create(OrganizationCreateRequestDto requestDto){
        OrganizationEntity org = orgMapper.createRequestDtoToEntity(requestDto);

        if(requestDto.getParentId()!=null){
            OrganizationEntity orgParent = orgRepository.findById(requestDto.getParentId()).orElseThrow(() -> new AppException(ApiResponseStatus.PARENT_ORGANIZATION_NOT_EXISTS));
            org.setParent(orgParent);
        }

        return orgMapper.entityToResponseDto(orgRepository.save(org));
    }

    @PreAuthorize("@perm.canAccess(#id, 'ORG', 'UPDATE',null)")
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

    @PreAuthorize("@perm.canAccess(#id, 'ORG', 'DELETE',null)")
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
}