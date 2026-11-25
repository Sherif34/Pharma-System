package com.pharma.accesscontrol;

import java.util.List;

public class AccessControlService {

    private final IPermissionEnforcement permissions;
    private final RoleManagementService roles;

    public AccessControlService(IPermissionEnforcement permissions,
                                RoleManagementService roles) {
        this.permissions = permissions;
        this.roles = roles;
    }

    public RoleAssignment assignRoleToUser(String actorId, String userId, String roleId) {
        requireManageRoles(actorId);
        return roles.assignRoleToUser(userId, roleId);
    }

    public List<RoleAssignment> getRolesForUser(String actorId, String userId) {
        requireManageRoles(actorId);
        return roles.getRolesForUser(userId);
    }

    private void requireManageRoles(String actorId) {
        if (!permissions.hasPermission(actorId, "MANAGE_ROLES")) {
            throw new SecurityException("You do not have permission to manage roles.");
        }
    }
}
