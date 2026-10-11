# ReadNotes

A **read-only** Android client for iCloud Notes. Sign in with your Apple ID,
browse folders, search note contents, open notes, select and copy text — without
creating, editing, or deleting anything from the device.

Built on reverse-engineered CloudKit private-DB access and Apple's `topotext`
proto. Alpha-quality; expect rough edges.

### Acknowledgments & lineage

**ReadNotes** is an independent, read-only fork of
[Notes of Fruit](https://github.com/ericmigi/Notes-Of-Fruit) by
[ericmigi](https://github.com/ericmigi).
Huge thanks to the original project for the CloudKit and proto foundation.

## Screenshots

<img height="800" alt="Screenshot_1777485511" src="https://github.com/user-attachments/assets/58914e12-889b-44f5-8515-f842d12f4ac2" />

## Install

Grab a signed APK from the [Releases page](../../releases) and side-load it onto
an Android 8.0+ device. Read the disclaimer below first.

> **Note for early v0.0.1 users:** v0.0.1 was signed with an unstable CI key,
> so upgrading directly to v0.0.2+ will fail with "Something went wrong / app
> could not be installed." **Uninstall v0.0.1 first**, then install v0.0.2.
> From v0.0.2 onwards every release uses a pinned project-owned key, so
> future upgrades install cleanly.

To build from source, see [Build / run](#build--run).

## Use at your own risk

This is **alpha-quality software**. The UI is read-only, but it still talks to
your iCloud account. It might:

- stop working at any time if Apple changes their API
- fail to decode some modern, locked, or collaborative notes
- expose note titles/contents on a device you unlock with your Apple ID

It is **unaffiliated with Apple**. There is no warranty of any kind. The first
launch blocks behind a dialog you have to acknowledge.

## Maintenance & contributing

I use ReadNotes myself and plan to keep maintaining it. Bug reports, ideas, and
pull requests are welcome — especially fixes you can ship with a PR.

If you reverse-engineer something new (a proto edge case, a CloudKit quirk), a
short note in the PR description helps the next person.

## What works

| Capability | Status |
|---|---|
| Sign in with Apple ID (cookie-based WebView) | Works |
| List notes (recents) | Works |
| Folder navigation (drawer, trash filter) | Works |
| Read note content (`NOTE_STORE` / topotext) | Works for common notes |
| Full-text search (title, snippet, body) | Works — body index cached on disk |
| Select & copy text | Works |
| Share out (Android intent chooser) | Works (plaintext) |
| Tables | Rendered as a grid when `MergeableData` decodes |
| Images | Fetched and shown inline when Media/Asset resolves |
| Other attachments | Labeled placeholder |
| Paragraph + inline formatting (display) | Title / headings / lists / checkboxes; bold / italic / underline / strikethrough; links |
| Password-locked notes | Shown with a lock icon; open shows an explanation (no unlock) |
| Light / dark / system theme | Works |

Write paths from the upstream codebase may still exist under `proto/` and
`debug/`, but the ReadNotes UI does not expose create, edit, or delete.

## Limitations

- **Password-locked notes** cannot be opened. CloudKit returns ciphertext; this
  app does not implement Apple's Notes password crypto.
- **Pinned notes** are not available via the CloudKit web API used here (pins
  appear to be local to Apple's clients).
- **Modern / collaborative note shapes** may fail to decode. Unsupported bodies
  are refused rather than corrupted.
- **No push / background sync.** The list refreshes when you open the app or
  pull refresh. Search body index persists across launches and only re-fetches
  notes that changed.
- **Attachment coverage is incomplete.** Sketches, maps, and similar types show
  as placeholders.
- **Not a sync client.** Do not expect offline-first behavior or conflict merge.

## Code tour

```
app/src/main/java/com/readnotes/
├── MainActivity.kt            Edge-to-edge shell, theme, splash
├── AlphaWarning.kt            First-launch disclaimer (DataStore)
├── SearchBodyIndexStore.kt    On-disk cache for full-text search bodies
├── auth/                      WebView cookie harvest, session, device identity
├── client/NotesClient.kt      CloudKit HTTP: recents, lookup, folders, assets
├── proto/
│   ├── NoteBodyLoader.kt      Resolve note body + attachments for display
│   ├── NoteBodyEditor.kt      Decode topotext → visible text + attribute runs
│   ├── MergeableDataDecoder.kt Table CRDT → 2D grid
│   ├── ProtobufWire.kt        Hand-rolled protobuf wire codec
│   └── Gzip.kt                gzip / zlib helpers
├── theme/                     Colors, typography, theme prefs
├── debug/DebugReceiver.kt     Optional adb broadcast helpers
└── ui/ReadNotesApp.kt         Compose UI: list, search, detail, drawer
```

Useful starting points: `NotesClient.kt` (wire calls), `NoteBodyLoader.kt`
(how a note becomes UI), `ReadNotesApp.kt` (screens and search index).

## Build / run

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.readnotes/.MainActivity
```

You need an Android emulator or device. Sign in with an Apple ID that has Notes
enabled in iCloud. The first launch uses the WebView auth flow.

## On "ENCRYPTED" field names

CloudKit labels fields like `TitleEncrypted`, `SnippetEncrypted`, and
`TextDataEncrypted` as encrypted types, but over the wire they are typically
**base64 UTF-8** (title/snippet) or **gzipped protobuf** (body) — not a
client-side crypto layer this app unwraps. iCloud **Advanced Data Protection**
is a separate mechanism; password-**locked** notes (the lock icon) are different
again and are not readable here.

## License

[0BSD](LICENSE) — use it for anything, no attribution required.

## Disclaimer

Unaffiliated with Apple. Uses CloudKit Web Services with the user's own
credentials to read notes that account already can access. Proto layouts were
inferred by observing the user's own data; no Apple-internal docs or binaries
were used.

Don't redistribute other people's note captures. Don't encourage device or
credential sharing.
