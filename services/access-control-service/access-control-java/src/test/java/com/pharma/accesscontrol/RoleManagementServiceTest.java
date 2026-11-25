    package com.pharma.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private RoleAssignmentRepository repository;

    @InjectMocks
    private RoleManagementService service;

    @Test
    void assignsRoleToUser() {
        RoleAssignment assignment = new RoleAssignment("U-014", "R-CSA");

        when(repository.save(assignment)).thenReturn(assignment);

        RoleAssignment result = service.assignRoleToUser("U-014", "R-CSA");

        assertEquals(assignment, result);
        verify(repository).save(assignment);
    }
}
@Test
void retrievesRolesForUser() {
    RoleAssignment assignment = new RoleAssignment("U-014", "R-CSA");
    var expected = java.util.List.of(assignment);
    when(repository.findByUserId("U-014")).thenReturn(expected);

    assertEquals(expected, service.getRolesForUser("U-014"));
    verify(repository).findByUserId("U-014");
}
