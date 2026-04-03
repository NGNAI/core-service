package ai.service;

import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.enums.PermissionScope;
import ai.model.PermissionGrantModel;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service("perm")
public class PermissionCheckerService {
    OrganizationService organizationService;
    OrganizationUserRoleService ourService;

    public boolean canAccess(UUID targetOrg, String resource, String action, String targetResource){
        List<PermissionGrantModel> permissions = ourService.getPermissionGrant(JwtUtil.getUserId(), JwtUtil.getOrgId());

        if(permissions==null || permissions.isEmpty())
            return false;

        for(PermissionGrantModel permission : permissions) {
            if(!matchResourceAction(permission,PermissionResource.valueOf(resource),PermissionAction.valueOf(action),targetResource!=null ? PermissionResource.valueOf(targetResource) : null))
                continue;

            switch (permission.getScope()){
                case ALL -> {
                    return true;
                }
                case OWN -> {
                    if(permission.getOrgId().equals(targetOrg))
                        return true;
                }
                case DESCENDANT -> {
                    if(permission.getOrgId().equals(targetOrg))
                        return true;
                    if(organizationService.isDescendant(permission.getOrgId(),targetOrg))
                        return true;
                }
            }
        }

        return false;
    }

    private boolean matchResourceAction(PermissionGrantModel p, PermissionResource resource, PermissionAction action, PermissionResource targetResource) {
        boolean resourceMatch =
                p.getResource().equals(resource) || p.getResource().equals(PermissionResource.ALL);

        boolean actionMatch =
                p.getAction().equals(action) || p.getAction().equals(PermissionAction.ALL);

        boolean targetResourceMatch = p.getTargetResource() == null || p.getTargetResource().equals(targetResource);

        return resourceMatch && actionMatch && targetResourceMatch;
    }
}
