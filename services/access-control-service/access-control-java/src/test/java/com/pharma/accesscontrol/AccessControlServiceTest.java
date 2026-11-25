package com.pharma.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AccessControlServiceTest {

    private final IPermissionEnforcement permissions = mock(IPermissionEnforcement.class);
    private final RoleAssignmentRepository repository = mock(RoleAssignmentRepository.class);
    private final AccessControlService service = new AccessControlService(
            permissions, new RoleManagementService(repository));

    @Test
    void checksPermissionBeforeSavingAssignment() {
        RoleAssignment assignment = new RoleAssignment("U-014", "R-CSA");
        when(permissions.hasPermission("U-001", "MANAGE_ROLES")).thenReturn(true);
        when(repository.save(assignment)).thenReturn(assignment);

        assertEquals(assignment, service.assignRoleToUser("U-001", "U-014", "R-CSA"));

        InOrder order = inOrder(permissions, repository);
        order.verify(permissions).hasPermission("U-001", "MANAGE_ROLES");
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
