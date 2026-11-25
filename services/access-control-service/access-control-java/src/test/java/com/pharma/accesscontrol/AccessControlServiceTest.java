package com.pharma.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AccessControlServiceTest {

    private final IPermissionEnforcement permissions = mock(IPermissionEnforcement.class);
    private final RoleAssignmentRepository repository = mock(RoleAssignmentRepository.class);
    private final AccessControlService service = new AccessControlService(
            permissions, new RoleManagementService(repository));

    @Test
    void checksPermissionAndLooksUpUserAndRoleBeforeSaving() {
        User user = new User("U-014", "staff14");
        Role role = new Role("R-CSA", "COUNTER_STAFF");
        RoleAssignment assignment = new RoleAssignment(user.userId(), role.roleId());

        when(permissions.hasPermission("U-001", "MANAGE_ROLES")).thenReturn(true);
        when(repository.findUserById(user.userId())).thenReturn(Optional.of(user));
        when(repository.findRoleById(role.roleId())).thenReturn(Optional.of(role));
        when(repository.save(assignment)).thenReturn(assignment);

        RoleAssignment result = service.assignRoleToUser(
                "U-001", user.userId(), role.roleId());

        assertEquals(assignment, result);

        InOrder order = inOrder(permissions, repository);
        order.verify(permissions).hasPermission("U-001", "MANAGE_ROLES");
        order.verify(repository).findUserById(user.userId());
        order.verify(repository).findRoleById(role.roleId());
        order.verify(repository).save(assignment);
    }

    @Test
    void deniesAssignmentWithoutManageRolesPermission() {
        when(permissions.hasPermission("U-014", "MANAGE_ROLES")).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> service.assignRoleToUser("U-014", "U-014", "R-CSA"));

        verify(permissions).hasPermission("U-014", "MANAGE_ROLES");
        verifyNoInteractions(repository);
    }
}