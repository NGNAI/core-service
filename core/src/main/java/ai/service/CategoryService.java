package ai.service;

import ai.dto.own.response.PermissionActionResponseDto;
import ai.dto.own.response.PermissionResourceResponseDto;
import ai.dto.own.response.PermissionScopeResponseDto;
import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.enums.PermissionScope;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class CategoryService {
    public List<PermissionResourceResponseDto> getPermissionResource() {
        return Arrays.stream(PermissionResource.values())
                .map(resource -> {
                    PermissionResourceResponseDto dto = new PermissionResourceResponseDto();
                    dto.setKey(resource.getKey());
                    dto.setName(resource.getName());
                    return dto;
                })
                .toList();
    }

    public List<PermissionActionResponseDto> getPermissionAction() {
        return Arrays.stream(PermissionAction.values())
                .map(action -> {
                    PermissionActionResponseDto dto = new PermissionActionResponseDto();
                    dto.setKey(action.getKey());
                    dto.setName(action.getName());
                    return dto;
                })
                .toList();
    }

    public List<PermissionScopeResponseDto> getPermissionScope() {
        return Arrays.stream(PermissionScope.values())
                .map(scope -> {
                    PermissionScopeResponseDto dto = new PermissionScopeResponseDto();
                    dto.setKey(scope.getKey());
                    dto.setName(scope.getName());
                    return dto;
                })
                .toList();
    }
}
