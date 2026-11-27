package edu.cse454.pharma;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.cse454.pharma.Scenarios.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * FR13 Maintain Customer Credit (and FR14 refill reminders).
 * Fixtures  = objects of the Part A object diagram (C-014, CE-001 +100, CE-002 -40, U-02).
 * Mocks     = the «mock» object access and the repository.
 * Verify    = the numbered messages of the collaboration diagram (1.1 ... 3.3).
 * Tests of RefillReminder cover the transitions of the state diagram.
 */
class CustomerRegisterServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 24, 18, 30);

    private IAccessControl access;
    private CustomerRegisterRepository repo;
    private CreditRegisterManager crm;
    private RefillReminderManager rrm;
    private RefillReminder reminder;

    @BeforeEach
    void setUp() {
        access = mock(IAccessControl.class);
        repo = mock(CustomerRegisterRepository.class);
        Iterator<String> entryIds = List.of("CE-001", "CE-002", "CE-003").iterator();
        crm = new CreditRegisterManager(access, repo, entryIds::next, () -> TODAY);
        rrm = new RefillReminderManager(access, repo, () -> "RR-001", () -> CREATED_AT);
        reminder = new RefillReminder("RR-001", "C-014", DUE_DATE, "U-02", CREATED_AT);
    }

    // ---------- Collaboration diagram: messages 1, 1.1, 1.2, 1.3 ----------

    @Test
    void recordCreditPlus100ChecksPermissionCreatesEntryAndSaves() {
        when(access.checkPermission("U-02", "recordCredit")).thenReturn(true);

        CreditEntry e1 = crm.recordCredit("U-02", "C-014", 100);

        InOrder order = inOrder(access, repo);
        order.verify(access).checkPermission("U-02", "recordCredit");        // 1.1
        ArgumentCaptor<CreditEntry> saved = ArgumentCaptor.forClass(CreditEntry.class);
        order.verify(repo).saveEntry(saved.capture());                        // 1.3
        verifyNoMoreInteractions(access, repo);

        assertSame(e1, saved.getValue());                                     // 1.2 created entry
        assertEquals("CE-001", e1.entryId());
        assertEquals("C-014", e1.customerRef());
        assertEquals(100, e1.amount());
        assertEquals(TODAY, e1.entryDate());
        assertEquals("U-02", e1.recordedBy());
    }

    @Test
    void recordCreditMinus40IsSavedAsSecondEntry() {
        when(access.checkPermission("U-02", "recordCredit")).thenReturn(true);
        crm.recordCredit("U-02", "C-014", 100);

        CreditEntry e2 = crm.recordCredit("U-02", "C-014", -40);              // 2, 2.1, 2.2, 2.3

        assertEquals("CE-002", e2.entryId());
        assertEquals(-40, e2.amount());
        verify(access, times(2)).checkPermission("U-02", "recordCredit");     // 1.1 and 2.1
        verify(repo).saveEntry(e2);                                           // 2.3
    }

    @Test
    void recordCreditDeniedDoesNotSave() {
        when(access.checkPermission("U-99", "recordCredit")).thenReturn(false);

        assertThrows(SecurityException.class, () -> crm.recordCredit("U-99", "C-014", 100));

        verifyNoInteractions(repo);
    }

    @Test
    void recordCreditOfZeroIsRejected() {
        when(access.checkPermission("U-02", "recordCredit")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> crm.recordCredit("U-02", "C-014", 0));

        verify(repo, never()).saveEntry(any());
    }

    // ---------- Collaboration diagram: full scenario 1, 2, 3 (balance = 60) ----------

    @Test
    void plus100MinusFortyGivesBalance60() {
        when(access.checkPermission(eq("U-02"), anyString())).thenReturn(true);

        CreditEntry e1 = crm.recordCredit("U-02", "C-014", 100);
        CreditEntry e2 = crm.recordCredit("U-02", "C-014", -40);
        when(repo.findEntries("C-014")).thenReturn(List.of(e1, e2));

        int balance = crm.getBalance("U-02", "C-014");

        assertEquals(60, balance);                                            // 3.3
        verify(access, times(2)).checkPermission("U-02", "recordCredit");     // 1.1, 2.1
        verify(access).checkPermission("U-02", "getBalance");                 // 3.1
        InOrder order = inOrder(repo);
        order.verify(repo).saveEntry(e1);                                     // 1.3
        order.verify(repo).saveEntry(e2);                                     // 2.3
        order.verify(repo).findEntries("C-014");                              // 3.2
    }

    @Test
    void balanceIsZeroWhenCustomerHasNoEntries() {
        when(access.checkPermission("U-02", "getBalance")).thenReturn(true);
        when(repo.findEntries("C-014")).thenReturn(List.of());

        assertEquals(0, crm.getBalance("U-02", "C-014"));
    }

    @Test
    void balanceTreatsMissingEntryListAsZero() {
        when(access.checkPermission("U-02", "getBalance")).thenReturn(true);
        when(repo.findEntries("C-014")).thenReturn(null);

        assertEquals(0, crm.getBalance("U-02", "C-014"));
    }

    @Test
    void getBalanceDeniedDoesNotReadEntries() {
        when(access.checkPermission("U-99", "getBalance")).thenReturn(false);

        assertThrows(SecurityException.class, () -> crm.getBalance("U-99", "C-014"));

        verifyNoInteractions(repo);
    }

    // ---------- Object diagram: fixtures hold the diagram values ----------

    @Test
    void fixturesMatchObjectDiagram() {
        Customer c = new Customer("C-014", LocalDate.of(2026, 11, 1));
        CreditEntry e1 = new CreditEntry("CE-001", "C-014", 100, TODAY, "U-02");
        CreditEntry e2 = new CreditEntry("CE-002", "C-014", -40, TODAY, "U-02");

        assertEquals("C-014", c.customerRef());
        assertEquals(LocalDate.of(2026, 11, 1), c.createdDate());
        assertEquals("CE-001", e1.entryId());
        assertEquals(100, e1.amount());
        assertEquals("CE-002", e2.entryId());
        assertEquals(-40, e2.amount());
        assertEquals(c.customerRef(), e1.customerRef());
        assertEquals(c.customerRef(), e2.customerRef());
        assertEquals(60, e1.amount() + e2.amount());
    }

    // ---------- RefillReminderManager messages ----------

    @Test
    void saveReminderChecksPermissionThenSavesNotDueReminder() {
        when(access.checkPermission("U-02", "saveReminder")).thenReturn(true);

        RefillReminder r = rrm.saveReminder("U-02", "C-014", DUE_DATE);

        InOrder order = inOrder(access, repo);
        order.verify(access).checkPermission("U-02", "saveReminder");
        order.verify(repo).saveReminder(r);
        assertEquals(ReminderStatus.NOT_DUE, r.status());
        assertEquals("RR-001", r.reminderId());
        assertEquals("C-014", r.customerRef());
        assertEquals(DUE_DATE, r.dueDate());
        assertEquals("U-02", r.createdBy());
        assertEquals(CREATED_AT, r.createdAt());
    }

    @Test
    void saveReminderDeniedDoesNotSave() {
        when(access.checkPermission("U-99", "saveReminder")).thenReturn(false);

        assertThrows(SecurityException.class, () -> rrm.saveReminder("U-99", "C-014", DUE_DATE));

        verifyNoInteractions(repo);
    }

    @Test
    void listDueRemindersReturnsRepositoryResult() {
        when(access.checkPermission("U-02", "listDueReminders")).thenReturn(true);
        when(repo.findRemindersDue(DUE_DATE)).thenReturn(List.of(reminder));

        List<RefillReminder> result = rrm.listDueReminders("U-02", DUE_DATE);

        assertEquals(List.of(reminder), result);
        verify(repo).findRemindersDue(DUE_DATE);
    }

    @Test
    void listDueRemindersDeniedDoesNotQuery() {
        when(access.checkPermission("U-99", "listDueReminders")).thenReturn(false);

        assertThrows(SecurityException.class, () -> rrm.listDueReminders("U-99", DUE_DATE));

        verifyNoInteractions(repo);
    }

    // ---------- State diagram: valid transitions ----------

    @Test
    void newReminderStartsNotDue() {
        assertEquals(ReminderStatus.NOT_DUE, reminder.status());
    }

    @Test
    void isDueOnIsFalseBeforeAndTrueOnOrAfterDueDate() {
        assertFalse(reminder.isDueOn(DUE_DATE.minusDays(1)));
        assertTrue(reminder.isDueOn(DUE_DATE));
        assertTrue(reminder.isDueOn(DUE_DATE.plusDays(1)));
    }

    @Test
    void notDueToDueWhenDateReachesDueDate() {
        assertEquals(ReminderStatus.DUE, reminder.evaluate(DUE_DATE));
        assertEquals(ReminderStatus.DUE, reminder.status());
    }

    // ---------- State diagram: invalid transitions must be refused ----------

    @Test
    void notDueStaysNotDueBeforeDueDate() {
        assertEquals(ReminderStatus.NOT_DUE, reminder.evaluate(DUE_DATE.minusDays(1)));
        assertEquals(ReminderStatus.NOT_DUE, reminder.status());
    }

    @Test
    void dueCannotGoBackToNotDue() {
        reminder.evaluate(DUE_DATE);

        assertThrows(IllegalStateException.class, () -> reminder.markNotDue());
        assertEquals(ReminderStatus.DUE, reminder.status());
    }

    @Test
    void dueStaysDueEvenWhenEvaluatedWithAnEarlierDate() {
        reminder.evaluate(DUE_DATE);

        assertEquals(ReminderStatus.DUE, reminder.evaluate(DUE_DATE.minusDays(10)));
    }

    @Test
    void notDueCannotBeMarkedNotDue() {
        assertThrows(IllegalStateException.class, () -> reminder.markNotDue());
        assertEquals(ReminderStatus.NOT_DUE, reminder.status());
    }

    @Test
    void reminderWithoutDueDateIsRefused() {
        assertThrows(NullPointerException.class,
                () -> new RefillReminder("RR-002", "C-014", null, "U-02", CREATED_AT));
    }
}
