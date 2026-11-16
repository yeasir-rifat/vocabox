# Vocabox (Android, Kotlin + Jetpack Compose)

Vocabulary trainer: word list, flashcards with spaced repetition, quizzes (step and list, PDF report),
word groups, favourites and mistakes, streaks and stats, daily word notification.
Account login is planned and not part of this version.

## Build
- JDK 17, Android SDK 35.
- Fonts (DM Sans, Noto Serif Bengali) are not stored in git. The Gradle task `downloadFonts` fetches them
  automatically on the first build (needs `bash`, `curl`, `jq` and network access). To do it by hand:
  `bash scripts/download_fonts.sh`.
- This repo has no Gradle wrapper jar. Use an installed Gradle 8.9 (`gradle :app:assembleDebug`), open the
  project in Android Studio, or run `gradle wrapper --gradle-version 8.9` once to create `gradlew`.
- Checks: `gradle :app:testDebugUnitTest :app:lintDebug`. GitHub Actions runs unit tests, lint, a debug build and an
  unsigned release (R8) build on every push.
- Room schemas are exported to `app/schemas`. Commit them with every database change.

## Status
Not compiled or run in the environment where the latest changes were made (no Android SDK). Run the checks above
before merging, and test on a device: light/dark, small phone, tablet, landscape, 200% font, TalkBack.

## Architecture
`ui/` Compose screens and ViewModels (Hilt), `domain/` pure Kotlin logic with unit tests, `data/` Room, DataStore,
repositories and the words.json importer, `worker/` daily word notification.

## Behaviour worth knowing
- **Words**: `assets/words.json` is imported on first launch and whenever its `version` increases. Words removed
  from the file are removed from the database together with their progress and favourites. A loader is shown until
  the import finishes, and a retry button if it fails with an empty database.
- **Daily word**: pinned per calendar day in DataStore, so Home and the notification always show the same word.
  Home, due counts and today's progress refresh at midnight.
- **Reminder**: each run schedules the next one for the next local HH:mm (survives Doze, DST). A receiver re-plans
  it when the time zone or clock changes. The notification permission is requested when the reminder is switched on.
- **Quiz**: the timers pause while the app is in the background. A mixed quiz saves one result row per mode.
  Interchangeable prepositions (in/into/at, on/over/upon, ...) are never offered next to the right answer.
- **Search**: matches word, Bangla meaning, English meaning and synonyms; `%` and `_` are matched literally.
- **Reset progress**: clears review progress, mistakes, quiz history, streaks and read marks. Words and favourites stay.
- **Saved**: removing a favourite or a mistake shows an Undo snackbar. The word sheet is a modal bottom sheet and
  survives rotation.

## Not done yet
- Account login and the profile page (`ui/components/ProfileCard.kt` is ready and hidden until then).
- Flashcard Again/Hard/Good/Easy grades, undo of the last answer, user-added words, CSV/JSON import and export,
  progress backup file, per-mode and per-topic stats charts, TTS voice choice.
- A larger word list: `words.json` has 96 entries (30 plain words), so Synonym and Antonym quizzes have small pools.

## Design system
Brand palette from the app icon (maroon `#A44A49`, cream `#FBF7F2`, espresso `#1A1211`, gold accent), explicit type
scale with generous line heights for Bangla, spacing tokens in `ui/theme/Spacing.kt`, one icon family in
`ui/components/AppIcons.kt`, shared components in `ui/components`.
