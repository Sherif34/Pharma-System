package edu.cse454.pharma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * customer-register-service (FR13 Maintain Customer Credit, FR14 Refill Reminders).
 * Mirrors the Part A class diagram: CreditRegisterManager and RefillReminderManager
 * use IAccessControl (required interface) and CustomerRegisterRepository.
 */
public final class Scenarios {

    private Scenarios() {}

    // ---------- Interfaces (class diagram) ----------

    public interface IAccessControl {
        boolean checkPermission(String staff, String action);
    }

    public interface CustomerRegisterRepository {
        CreditEntry saveEntry(CreditEntry entry);
        List<CreditEntry> findEntries(String customerRef);
        RefillReminder saveReminder(RefillReminder reminder);
        List<RefillReminder> findRemindersDue(LocalDate date);
    }

    // ---------- Domain classes ----------

    public static final class Customer {
        private final String customerRef;
        private final LocalDate createdDate;

        public Customer(String customerRef, LocalDate createdDate) {
            this.customerRef = Objects.requireNonNull(customerRef);
            this.createdDate = Objects.requireNonNull(createdDate);
        }

        public String customerRef() { return customerRef; }
        public LocalDate createdDate() { return createdDate; }
    }

    public static final class CreditEntry {
        private final String entryId;
        private final String customerRef;
        private final int amount; // signed: + credit, - use of credit
        private final LocalDate entryDate;
        private final String recordedBy;

        public CreditEntry(String entryId, String customerRef, int amount,
                           LocalDate entryDate, String recordedBy) {
            this.entryId = entryId;
            this.customerRef = customerRef;
            this.amount = amount;
            this.entryDate = entryDate;
            this.recordedBy = recordedBy;
        }

        public String entryId() { return entryId; }
        public String customerRef() { return customerRef; }
        public int amount() { return amount; }
        public LocalDate entryDate() { return entryDate; }
        public String recordedBy() { return recordedBy; }
    }

    public enum ReminderStatus { NOT_DUE, DUE }

    public static final class RefillReminder {
        private final String reminderId;
        private final String customerRef;
        private final LocalDate dueDate;
        private final String createdBy;
        private final LocalDateTime createdAt;
        private ReminderStatus status = ReminderStatus.NOT_DUE;

        public RefillReminder(String reminderId, String customerRef, LocalDate dueDate,
                              String createdBy, LocalDateTime createdAt) {
            this.reminderId = reminderId;
            this.customerRef = customerRef;
            this.dueDate = Objects.requireNonNull(dueDate, "dueDate");
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        public String reminderId() { return reminderId; }
        public String customerRef() { return customerRef; }
        public LocalDate dueDate() { return dueDate; }
        public String createdBy() { return createdBy; }
        public LocalDateTime createdAt() { return createdAt; }
        public ReminderStatus status() { return status; }

        /** isDueOn(date): true when the selected date has reached the due date. */
        public boolean isDueOn(LocalDate date) {
            return !date.isBefore(dueDate);
        }

        /** State diagram: NOT DUE -> DUE when isDueOn(date) = true. DUE is final. */
        public ReminderStatus evaluate(LocalDate date) {
            if (status == ReminderStatus.NOT_DUE && isDueOn(date)) {
                status = ReminderStatus.DUE;
            }
            return status;
        }

        /** Not allowed by the state diagram: nothing goes back to NOT DUE. */
        public void markNotDue() {
            throw new IllegalStateException("No transition returns to NOT_DUE (current: " + status + ")");
        }
    }

    // ---------- Managers ----------

    public static final class CreditRegisterManager {
        private final IAccessControl access;
        private final CustomerRegisterRepository repo;
        private final Supplier<String> nextEntryId;
        private final Supplier<LocalDate> today;

        public CreditRegisterManager(IAccessControl access, CustomerRegisterRepository repo,
                                     Supplier<String> nextEntryId, Supplier<LocalDate> today) {
            this.access = access;
            this.repo = repo;
            this.nextEntryId = nextEntryId;
            this.today = today;
        }

        /** Messages 1 / 2: recordCredit. 1.1 checkPermission, 1.2 create entry, 1.3 saveEntry. */
        public CreditEntry recordCredit(String staff, String customerRef, int amount) {
            if (!access.checkPermission(staff, "recordCredit")) {
                throw new SecurityException(staff + " may not record credit");
            }
            if (amount == 0) {
                throw new IllegalArgumentException("amount must not be zero");
            }
            CreditEntry entry = new CreditEntry(nextEntryId.get(), customerRef, amount, today.get(), staff);
            repo.saveEntry(entry);
            return entry;
        }

        /** Message 3: getBalance. 3.1 checkPermission, 3.2 findEntries, 3.3 calculateBalance. */
        public int getBalance(String staff, String customerRef) {
            if (!access.checkPermission(staff, "getBalance")) {
                throw new SecurityException(staff + " may not read balance");
            }
            return calculateBalance(repo.findEntries(customerRef));
        }

        private int calculateBalance(List<CreditEntry> entries) {
            int sum = 0;
            if (entries != null) {
                for (CreditEntry e : entries) {
                    sum += e.amount();
                }
            }
            return sum;
        }
    }

    public static final class RefillReminderManager {
        private final IAccessControl access;
        private final CustomerRegisterRepository repo;
        private final Supplier<String> nextReminderId;
        private final Supplier<LocalDateTime> now;

        public RefillReminderManager(IAccessControl access, CustomerRegisterRepository repo,
                                     Supplier<String> nextReminderId, Supplier<LocalDateTime> now) {
            this.access = access;
            this.repo = repo;
            this.nextReminderId = nextReminderId;
            this.now = now;
        }

        public RefillReminder saveReminder(String staff, String customerRef, LocalDate dueDate) {
            if (!access.checkPermission(staff, "saveReminder")) {
                throw new SecurityException(staff + " may not save reminders");
            }
            RefillReminder reminder = new RefillReminder(nextReminderId.get(), customerRef,
                    dueDate, staff, now.get());
            repo.saveReminder(reminder);
            return reminder;
        }

        public List<RefillReminder> listDueReminders(String staff, LocalDate onOrBefore) {
            if (!access.checkPermission(staff, "listDueReminders")) {
                throw new SecurityException(staff + " may not list reminders");
            }
            return repo.findRemindersDue(onOrBefore);
        }
    }
}
