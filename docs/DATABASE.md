# Database and persistence

Reminder uses Room as its local persistence layer.

## Database definition

The main database is declared in `app/src/main/java/com/thelastecho/reminder/data/local/ReminderDatabase.kt`.

Current schema:

- `ReminderEntity`
- `SubTaskEntity`
- `CategoryEntity`

The database version is `5` and `exportSchema` is disabled.

## Main entities

### `ReminderEntity`

This table stores the core reminder data:

- title, notes, due timestamp
- completion state and completion date
- priority level and repeat interval ID
- category reference
- image URI
- notification style override
- created timestamp
- soft-delete fields and expiration timestamp

### `SubTaskEntity`

A subtask belongs to a reminder and stores:

- title
- completion state
- order index
- reminder foreign key

### `CategoryEntity`

The category table stores:

- name
- color ARGB value
- icon name

## Relationships

- reminders can reference a category through `categoryId`;
- subtasks belong to a reminder via `reminderId`;
- on category deletion, `FOREIGN KEY ... SET NULL` is used for linked reminders.

## DAO responsibilities

The main DAO is `ReminderDao` and it handles:

- active, completed, and deleted reminder streams
- reminder queries by category and ID
- backup export/import of data
- save update and completion toggling
- soft delete, restore, purge, and expiration cleanup
- subtask creation and update

## Migration history

The current migrations are:

- `1 -> 2`: adds `isDeleted` and `deletedAt`
- `2 -> 3`: adds `notificationStyleId`
- `3 -> 4`: adds `expiresAt` and backfills it from `deletedAt`

This schema history is intentionally local and persistent across the current application lifecycle.

## Data storage strategy

The application persists:

- reminders and subtasks in Room;
- preferences in DataStore;
- attachments as files under the app’s private storage and Uris in Room;
- backups as `.reminderbackup` archives written to a user-selected SAF directory.

## Local retention rules

The database and repository logic implement:

- soft deletion rather than hard deletion;
- 90-day purge for deleted entries;
- optional retention for completed reminders;
- expired items are permanently deleted by WorkManager and app-start sweep.

## Backup and restore relation

The backup repository reads and writes Room data and then restores it carefully with validation checks. This is a portability feature, not a remote synchronization feature.

## Guidance for schema changes

If you modify the schema:

- add a migration with explicit SQL changes;
- verify existing rows still load correctly;
- test restore/export behavior and retention logic;
- avoid changing the existing local-first behavior of the app.

See also [docs/BACKUP_RESTORE.md](BACKUP_RESTORE.md), [docs/ARCHITECTURE.md](ARCHITECTURE.md), and [docs/ROADMAP.md](ROADMAP.md).
