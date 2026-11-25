package com.pharma.accesscontrol;

import java.util.List;
import java.util.Optional;

public interface RoleAssignmentRepository {

    Optional<User> findUserById(String userId);

    Optional<Role> findRoleById(String roleId);

    RoleAssignment save(RoleAssignment assignment);

    List<RoleAssignment> findByUserId(String userId);

    List<Role> getUserRoles(String userId);
}