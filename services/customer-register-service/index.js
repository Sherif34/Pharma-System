const express = require('express');

const app = express();
app.use(express.json());

// ==========================================
// Customer Register Service - FR13
// ==========================================

// Customer data
const customers = {
  'C-014': {
    customerRef: 'C-014',
    createdDate: '2026-11-01'
  }
};

// In-memory repository
class CustomerRegisterRepository {
  constructor() {
    this.creditEntries = [];
    this.refillReminders = [];
  }

  saveEntry(entry) {
    this.creditEntries.push(entry);
    return entry;
  }

  findEntries(customerRef) {
    return this.creditEntries.filter(
      entry => entry.customerRef === customerRef
    );
  }

  saveReminder(reminder) {
    this.refillReminders.push(reminder);
    return reminder;
  }

  findRemindersDue(date) {
    return this.refillReminders.filter(
      reminder => reminder.isDueOn(date)
    );
  }
}

// Access control
class AccessControl {
  checkPermission(staffId, action) {
    return staffId === 'U-02';
  }
}

// Credit Register Manager
class CreditRegisterManager {
  constructor(access, repo) {
    this.access = access;
    this.repo = repo;
  }

  recordCredit(customerRef, amount, staffId, entryId) {
    if (!this.access.checkPermission(staffId, 'recordCredit')) {
      throw new Error('Permission denied');
    }

    if (!customers[customerRef]) {
      throw new Error('Customer not found');
    }

    if (!Number.isFinite(amount) || amount === 0) {
      throw new Error('Invalid credit amount');
    }

    const entry = {
      entryId,
      customerRef,
      amount,
      entryDate: new Date().toISOString().slice(0, 10),
      recordedBy: staffId
    };

    return this.repo.saveEntry(entry);
  }

  getBalance(customerRef, staffId) {
    if (!this.access.checkPermission(staffId, 'getBalance')) {
      throw new Error('Permission denied');
    }

    if (!customers[customerRef]) {
      throw new Error('Customer not found');
    }

    const entries = this.repo.findEntries(customerRef);

    return this.calculateBalance(entries);
  }

  calculateBalance(entries) {
    return entries.reduce(
      (total, entry) => total + entry.amount,
      0
    );
  }
}

// Refill Reminder
class RefillReminder {
  constructor(reminderId, customerRef, dueDate, createdBy) {
    this.reminderId = reminderId;
    this.customerRef = customerRef;
    this.dueDate = dueDate;
    this.createdBy = createdBy;
    this.createdAt = new Date().toISOString();
  }

  isDueOn(date) {
    const selectedDate = new Date(date);
    const dueDate = new Date(this.dueDate);

    if (
      Number.isNaN(selectedDate.getTime()) ||
      Number.isNaN(dueDate.getTime())
    ) {
      return false;
    }

    return selectedDate >= dueDate;
  }

  getStatus(date) {
    return this.isDueOn(date) ? 'DUE' : 'NOT DUE';
  }
}

// Refill Reminder Manager
class RefillReminderManager {
  constructor(access, repo) {
    this.access = access;
    this.repo = repo;
  }

  saveReminder(customerRef, dueDate, staffId, reminderId) {
    if (!this.access.checkPermission(staffId, 'saveReminder')) {
      throw new Error('Permission denied');
    }

    if (!customers[customerRef]) {
      throw new Error('Customer not found');
    }

    if (
      !dueDate ||
      Number.isNaN(new Date(dueDate).getTime())
    ) {
      throw new Error('Invalid due date');
    }

    const reminder = new RefillReminder(
      reminderId,
      customerRef,
      dueDate,
      staffId
    );

    return this.repo.saveReminder(reminder);
  }

  listDueReminders(onOrBefore, staffId) {
    if (!this.access.checkPermission(staffId, 'listDueReminders')) {
      throw new Error('Permission denied');
    }

    if (Number.isNaN(new Date(onOrBefore).getTime())) {
      throw new Error('Invalid selected date');
    }

    return this.repo.findRemindersDue(onOrBefore).map(reminder => ({
      reminderId: reminder.reminderId,
      customerRef: reminder.customerRef,
      dueDate: reminder.dueDate,
      status: reminder.getStatus(onOrBefore)
    }));
  }
}

// Initialize service components
const repository = new CustomerRegisterRepository();
const accessControl = new AccessControl();

const creditManager = new CreditRegisterManager(
  accessControl,
  repository
);

const reminderManager = new RefillReminderManager(
  accessControl,
  repository
);

// ==========================================
// API Endpoints
// ==========================================

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: process.env.SERVICE_NAME || 'customer-register-service'
  });
});

// Get customer
app.get('/customers/:id', (req, res) => {
  const customer = customers[req.params.id];

  if (!customer) {
    return res.status(404).json({
      message: 'Customer not found'
    });
  }

  res.json(customer);
});

// Record credit entry
app.post('/customers/:id/credits', (req, res) => {
  try {
    const { entryId, amount, staffId } = req.body;

    const entry = creditManager.recordCredit(
      req.params.id,
      amount,
      staffId,
      entryId
    );

    res.status(201).json(entry);
  } catch (error) {
    const status = error.message === 'Customer not found'
      ? 404
      : error.message === 'Permission denied'
        ? 403
        : 400;

    res.status(status).json({ message: error.message });
  }
});

// Get customer balance
app.get('/customers/:id/balance', (req, res) => {
  try {
    const balance = creditManager.getBalance(
      req.params.id,
      req.query.staffId
    );

    res.json({
      customerRef: req.params.id,
      balance
    });
  } catch (error) {
    const status = error.message === 'Customer not found'
      ? 404
      : error.message === 'Permission denied'
        ? 403
        : 400;

    res.status(status).json({ message: error.message });
  }
});

// Save refill reminder
app.post('/customers/:id/reminders', (req, res) => {
  try {
    const { reminderId, dueDate, staffId } = req.body;

    const reminder = reminderManager.saveReminder(
      req.params.id,
      dueDate,
      staffId,
      reminderId
    );

    res.status(201).json({
      reminderId: reminder.reminderId,
      customerRef: reminder.customerRef,
      dueDate: reminder.dueDate,
      status: reminder.getStatus(new Date().toISOString().slice(0, 10))
    });
  } catch (error) {
    const status = error.message === 'Customer not found'
      ? 404
      : error.message === 'Permission denied'
        ? 403
        : 400;

    res.status(status).json({ message: error.message });
  }
});

// List reminders due on or before selected date
app.get('/reminders/due', (req, res) => {
  try {
    const reminders = reminderManager.listDueReminders(
      req.query.onOrBefore,
      req.query.staffId
    );

    res.json(reminders);
  } catch (error) {
    const status = error.message === 'Permission denied' ? 403 : 400;
    res.status(status).json({ message: error.message });
  }
});

// Export app for testing
if (require.main === module) {
  app.listen(process.env.PORT || 3000, () => {
    console.log('Customer Register Service listening');
  });
}

module.exports = app;
