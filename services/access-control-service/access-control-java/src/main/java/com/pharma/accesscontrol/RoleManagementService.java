package com.pharma.accesscontrol;

import java.util.List;

public class RoleManagementService {

    private final RoleAssignmentRepository repository;

    public RoleManagementService(RoleAssignmentRepository repository) {
        this.repository = repository;
    }

    public RoleAssignment assignRoleToUser(String userId, String roleId) {
        RoleAssignment assignment = new RoleAssignment(userId, roleId);
        return repository.save(assignment);
    }

    public List<RoleAssignment> getRolesForUser(String userId) {
        return repository.findByUserId(userId);
    }
}
