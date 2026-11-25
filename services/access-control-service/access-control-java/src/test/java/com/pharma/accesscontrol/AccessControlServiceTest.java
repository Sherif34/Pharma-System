package com.pharma.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AccessControlServiceTest {

    private final RoleAssignmentRepository repository = mock(RoleAssignmentRepository.class);
    private final AccessControlService service = new AccessControlService(
            new PermissionEnforcementService(repository),
            new RoleManagementService(repository));

    @Test
    void checksOwnerRoleAndLooksUpUserAndRoleBeforeSaving() {
        User user = new User("U-014", "staff14");
        Role role = new Role("R-CSA", "COUNTER_STAFF");
        Role ownerRole = new Role("R-OWNER", "PHARMACY_OWNER");
        RoleAssignment assignment = new RoleAssignment(user.userId(), role.roleId());

        when(repository.getUserRoles("U-001")).thenReturn(List.of(ownerRole));
        when(repository.findUserById(user.userId())).thenReturn(Optional.of(user));
        when(repository.findRoleById(role.roleId())).thenReturn(Optional.of(role));
        when(repository.save(assignment)).thenReturn(assignment);

        RoleAssignment result = service.assignRoleToUser(
                "U-001", user.userId(), role.roleId());

        assertEquals(assignment, result);

        InOrder order = inOrder(repository);
        order.verify(repository).getUserRoles("U-001");
        order.verify(repository).findUserById(user.userId());
        order.verify(repository).findRoleById(role.roleId());
        order.verify(repository).save(assignment);
    }

    @Test
    void deniesAssignmentWhenCallerIsCounterStaff() {
        Role staffRole = new Role("R-CSA", "COUNTER_STAFF");
        when(repository.getUserRoles("U-014")).thenReturn(List.of(staffRole));

        assertThrows(SecurityException.class,
                () -> service.assignRoleToUser("U-014", "U-014", "R-CSA"));

        verify(repository).getUserRoles("U-014");
        verifyNoMoreInteractions(repository);
    }
}