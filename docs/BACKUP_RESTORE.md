# Backup and restore

Reminder implements local backup and restore with portable `.reminderbackup` archives. The app does not provide cloud integration or network synchronization. The user chooses a destination through Android's Storage Access Framework (SAF).

## Format

The archive contains `manifest.json`, `database.json`, and optional `attachments/` files. The manifest records `formatVersion`, `appVersion`, `createdAt`, `databaseVersion`, `encrypted`, and `attachmentsIncluded`. Database JSON contains reminders, categories, subtasks, and the portable app preferences. Reminder exports include configurable recurrence intervals and limits and favorite state; older archives default these fields to the original recurrence behavior and not-favorite state. Photo references are stable attachment IDs, not device URIs.

Optional encryption wraps the complete ZIP payload in a versioned envelope. It uses PBKDF2-HMAC-SHA-256 with a per-backup random salt and AES-256-GCM with a per-backup random nonce. Restore requires only the backup password, not the originating device's Android Keystore.

Manual and automatic backups can use separate SAF destinations but retain distinct filename prefixes: `Reminder-Manual-` and `Reminder-Auto-`. Until a manual destination is chosen, manual backups use the configured automatic destination. Retention lists and deletes only `Reminder-Auto-` files, so manual backups are never automatically removed. Manual password protection is chosen per export and is not stored; the automatic password is stored separately in device-protected form for the worker.

## What is included

The export includes:

- reminder IDs and metadata
- completion and deletion timestamps
- reminder notification style and category references
- subtask rows and ordering
- embedded attachment bytes for photo-based reminders
- theme, accent, notification, persistence, and related local preferences

## File and attachment limits

The archive repository enforces limits:

- backup file max: 50 MB
- attachment max per file: 20 MB
- total attachments in a backup: 35 MB

Restore validates those limits and all archive paths/references before writing.

## Restore modes

The archive repository supports two restore modes:

- `MERGE`
- `REPLACE`

### MERGE

Merge mode:

- keeps existing rows for matching IDs;
- imports new reminders and subtasks;
- detects conflicts and reports them;
- leaves existing app preferences unchanged.

### REPLACE

Replace mode:

- wipes the local reminder and category data inside a Room transaction;
- inserts the backup content;
- restores relevant reminder preferences;
- removes app-owned attachments that are no longer referenced.

## Validation rules

Before writing data to Room, the backup is validated for:

- correct format name
- supported version
- valid category data
- valid reminder IDs and titles
- valid subtask IDs
- no duplicate IDs
- no missing parent reminder/subtask references
- no external image URI references (only embedded attachments)

This validation happens before mutation. Room changes are transactional; the importer stages attachment files and removes them if the database/preferences restore fails. Alarm scheduling occurs after the Room commit; failed alarm operations are listed in the restore report rather than presented as fully successful.

## Photos and attachments

Photo Picker images are copied immediately into `filesDir/attachments/`; Room stores relative paths. Existing picker URI references are migrated on app startup when readable. Unreadable references are preserved in the legacy `imageUri` column and counted in the settings warning; successfully migrated references clear that legacy column. New photos never populate the legacy URI column.

## Risk and rollback

The restore path tries to avoid partial writes:

- it parses and validates the archive before database mutation;
- it stages attachment files before the Room transaction;
- it restores the prior database snapshot if preference commit fails after Room commit;
- it rolls back preferences and deletes staged files on failure.

## Limitations

- WorkManager periodic execution is inexact and subject to Android background limits, including Doze;
- the user-selected SAF provider may not support listing/deleting children, in which case backup succeeds and retention reports a warning;
- the automatic worker's local password copy is wrapped by the source device's Keystore; reinstalling or losing that key requires entering the password again, while cross-device restore still uses only the password;
- custom alarm sound files are not bundled; an unavailable URI falls back to the system default and is reported;
- this is a local data portability feature, not cloud backup or synchronization.

See also [docs/DATABASE.md](DATABASE.md) and [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md).
