# Backup and restore

Reminder implements local backup and restore in a portable JSON format. This is a local data portability feature, not a remote sync or cloud backup feature.

## Format

The backup format is:

- `format = "the-last-echo-reminder-backup"`
- `version = 1`

The exported JSON includes:

- reminders
- categories
- subtasks
- attachments
- app preferences relevant to the current app state

## What is included

The export includes:

- reminder IDs and metadata
- completion and deletion timestamps
- reminder notification style and category references
- subtask rows and ordering
- embedded attachment bytes for photo-based reminders
- theme, accent, notification, persistence, and related local preferences

## File and attachment limits

The repository enforces limits:

- backup file max: 50 MB
- attachment max per file: 20 MB
- total attachments in a backup: 35 MB

The restore logic validates those limits before writing.

## Restore modes

The repository supports two restore modes:

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

This validation happens before mutation, so the restore path does not write partial state.

## Photos and attachments

Photos are not kept as a picker URI from another device. Instead, the export embeds the attachment bytes as Base64 and writes them to app-private storage on restore. This keeps the backup portable and self-contained.

The code also cleans up temporary app-owned files if they are not referenced after a successful restore.

## Risk and rollback

The restore path tries to avoid partial writes:

- it parses and validates everything first;
- it stages attachment files before the transaction;
- it rolls back preference changes on failure;
- it deletes temporary files if restore fails.

## Limitations

- this is a local backup, not a cloud or server-based sync mechanism;
- it does not implement conflict resolution beyond the merge/replace report logic;
- it does not manage remote attachments or a server-side backup repository.

See also [docs/DATABASE.md](DATABASE.md) and [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md).
