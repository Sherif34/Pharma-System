    package com.pharma.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private RoleAssignmentRepository repository;

    @InjectMocks
    private RoleManagementService service;

    @Test
    void assignsRoleToUser() {
        RoleAssignment assignment = new RoleAssignment("U-014", "R-CSA");
        when(repository.findUserById("U-014"))
        .thenReturn(Optional.of(new User("U-014", "staff14")));
when(repository.findRoleById("R-CSA"))
        .thenReturn(Optional.of(new Role("R-CSA", "COUNTER_STAFF")));

        when(repository.save(assignment)).thenReturn(assignment);

        RoleAssignment result = service.assignRoleToUser("U-014", "R-CSA");

        assertEquals(assignment, result);
        verify(repository).save(assignment);
    }
    @Test
void retrievesRolesForUser() {
    RoleAssignment assignment = new RoleAssignment("U-014", "R-CSA");
    var expected = java.util.List.of(assignment);
    when(repository.findByUserId("U-014")).thenReturn(expected);

    assertEquals(expected, service.getRolesForUser("U-014"));
    verify(repository).findByUserId("U-014");
}

        @Test
    void reportsFailureWhenSavingRoleAssignmentFails() {
        RoleAssignment assignment = new RoleAssignment("U-014", "R-CSA");
        when(repository.save(assignment))
                .thenThrow(new IllegalStateException("Could not save assignment"));

        var error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.assignRoleToUser("U-014", "R-CSA"));

        assertEquals("Could not save assignment", error.getMessage());
        verify(repository).save(assignment);
    }
}

