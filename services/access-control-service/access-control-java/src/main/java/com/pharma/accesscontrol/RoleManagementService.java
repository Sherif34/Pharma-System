package com.pharma.accesscontrol;

import java.util.List;

public class RoleManagementService {

    private final RoleAssignmentRepository repository;

    public RoleManagementService(RoleAssignmentRepository repository) {
        this.repository = repository;
    }

    public RoleAssignment assignRoleToUser(String userId, String roleId) {
    User user = repository.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + userId));

    Role role = repository.findRoleById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleId));

    return repository.save(new RoleAssignment(user.userId(), role.roleId()));
}

    public List<RoleAssignment> getRolesForUser(String userId) {
        return repository.findByUserId(userId);
    }
}
