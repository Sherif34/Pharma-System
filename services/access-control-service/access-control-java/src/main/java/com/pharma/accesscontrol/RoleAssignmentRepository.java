package com.pharma.accesscontrol;

import java.util.List;

public interface RoleAssignmentRepository {

    RoleAssignment save(RoleAssignment assignment);

    List<RoleAssignment> findByUserId(String userId);
}
