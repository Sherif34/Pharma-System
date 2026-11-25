package com.pharma.accesscontrol;

public class PermissionEnforcementService implements IPermissionEnforcement {

    private final RoleAssignmentRepository repository;

    public PermissionEnforcementService(RoleAssignmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        if (!"MANAGE_ROLES".equals(permission)) {
            return false;
        }

        return repository.getUserRoles(userId).stream()
                .anyMatch(role -> "PHARMACY_OWNER".equals(role.roleName()));
    }
}