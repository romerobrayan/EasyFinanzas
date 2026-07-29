# Text-to-speech

Tinto can speak a summary of a month's spending out loud. This document describes the
contract, the state machine the UI drives, the failure modes we handle, and how to replace
the on-device engine with a cloud provider.

Sprint 1 covers the speech layer only: no cloud provider, no LLM-generated narration, no
speech-to-text.

## Why there is an interface at all

`android.speech.tts.TextToSpeech` is a platform service with an asynchronous, callback-
driven lifecycle. If features called it directly, three things would rot:

- `core/domain` would import `android.*`, which the layer rules forbid.
- Every consumer would re-implement "wait for init, then wait for the utterance".
- Swapping in a cloud provider later would mean touching every call site.

So the domain owns a small contract, the data layer owns the engine, and Hilt joins them.
This mirrors how the capture pipeline already treats platform capabilities
(`NotificationCapture`, `SmsCapture` in `core/domain/repository/`).

## The contract

```kotlin
// core/domain/repository/SpeechSynthesizer.kt
interface SpeechSynthesizer {
    suspend fun speak(text: String, onStarted: () -> Unit = {}): SpeechOutcome
    fun stop()
    fun shutdown()
}
```

`speak` **suspends until the utterance finishes**. That is the central design choice: the
caller's coroutine job *is* the utterance's lifetime. Cancelling the job stops playback,
and there is no separate "am I still speaking?" flag that can drift out of sync with the
engine. It also makes the whole thing testable on the JVM — a fake is a handful of lines
with no Android and no Robolectric.

`onStarted` fires when audio actually begins, which is what separates `Preparing` from
`Speaking`. Engine initialisation and voice-data lookup happen *before* the first sound, and
on a cold start that gap is human-noticeable.

Failures are returned, not thrown:

```kotlin
// core/domain/model/SpeechOutcome.kt
sealed interface SpeechOutcome {
    data object Completed : SpeechOutcome
    data object Cancelled : SpeechOutcome
    data class Failed(val reason: SpeechFailure) : SpeechOutcome
}

enum class SpeechFailure {
    ENGINE_UNAVAILABLE,     // no TTS engine installed, or init returned ERROR
    LANGUAGE_NOT_SUPPORTED, // engine has no Spanish voice at all
    MISSING_VOICE_DATA,     // Spanish is known but its data is not downloaded
    SYNTHESIS_FAILED,       // engine accepted the utterance then errored
}
```

An enum rather than free-form text: the ViewModel maps each case to its own
`@StringRes` in an exhaustive `when`, so adding a failure mode later is a compile error
instead of a silent fallthrough to a generic message.

## States

`Cancelled` is deliberately distinct from `Completed`. A user who taps stop should not see
an error, and should not hear the summary again on a "replay when finished" path.

```
                 tap
    Idle ─────────────────────► Preparing
      ▲                            │  onStarted()
      │                            ▼
      │  Completed / Cancelled   Speaking
      ├────────────────────────────┤
      │                            │
      │        Failed(reason)      │
    Error ◄─────────────────────────
      │
      └── tap ──► Preparing   (a new attempt clears the error)
```

`Preparing` covers engine construction, `OnInitListener` firing, language selection, and
the queueing of the utterance. It is a real state, not a formality — a cold engine takes
roughly 200–500 ms, and it can fail.

The UI surface is a single control in the Dashboard header:

| State       | Control                                        |
| ----------- | ---------------------------------------------- |
| `Idle`      | play icon                                      |
| `Preparing` | indeterminate progress, same footprint as icon |
| `Speaking`  | stop icon                                      |
| `Error`     | play icon + the mapped message                 |

Tapping during `Preparing` or `Speaking` stops rather than starting a second utterance.

## Asynchronous initialisation

`TextToSpeech` is not usable until its `OnInitListener` fires, and the constructor starts
that work immediately. `AndroidTtsSynthesizer` handles it like this:

- The engine is created lazily on the first `speak`, not at injection time. Constructing it
  eagerly would bind a system service for users who never press play.
- Init is bridged into a `CompletableDeferred<TextToSpeech?>` guarded by a `Mutex`, so two
  concurrent `speak` calls await one initialisation instead of racing to build two engines.
- A **failed** init is not cached. The deferred is discarded so the next tap retries from
  scratch — the user may have installed voice data in between.
- A successful engine is kept warm for the rest of the owning scope.

Language selection tries `es-CO` first, then falls back to `es`, and maps the return codes:

| `setLanguage` result                     | Outcome                            |
| ---------------------------------------- | ---------------------------------- |
| `LANG_MISSING_DATA`                      | `Failed(MISSING_VOICE_DATA)`       |
| `LANG_NOT_SUPPORTED`                     | `Failed(LANGUAGE_NOT_SUPPORTED)`   |
| init returned `ERROR` / no engine present | `Failed(ENGINE_UNAVAILABLE)`      |

The utterance itself is a `suspendCancellableCoroutine` resumed by an
`UtteranceProgressListener`: `onStart` → `onStarted()`, `onDone` → `Completed`, `onStop` →
`Cancelled`, `onError` → `Failed(SYNTHESIS_FAILED)`. Cancelling the coroutine calls
`TextToSpeech.stop()` via `invokeOnCancellation`, so audio cuts immediately.

## Engine lifetime and `shutdown()`

`TextToSpeech` holds a binding to a system service and must be released with `shutdown()`.
This is genuinely awkward under Hilt and worth spelling out, because the obvious reading of
the code is wrong.

`AndroidTtsSynthesizer` is a `@Singleton` — it has to be, so that swapping providers is one
`@Binds` line. But **Hilt singletons have no destroy callback**, so nothing in the DI
container will ever call `shutdown()`.

The options, honestly:

| Option                            | Cost                                                      |
| --------------------------------- | --------------------------------------------------------- |
| Never shut down                   | Leaks an engine connection for the whole process lifetime |
| Rebuild per utterance             | Pays full init latency on every single tap                |
| Observe `ProcessLifecycleOwner`   | Correct, but adds `androidx.lifecycle:lifecycle-process`  |
| Let the consuming ViewModel own it | Correct today; a smell if a second consumer appears       |

**We chose the last one.** `DashboardViewModel.onCleared()` calls `stop()` then
`shutdown()`. The Dashboard is the only consumer, so it genuinely is the owning scope. The
engine stays warm across taps within a session and is released when the screen dies.

`shutdown()` is idempotent and nulls out the engine, so a later `speak()` transparently
re-initialises. That matters: the day a second screen wants speech, the failure mode is one
extra initialisation, not a crash against a dead engine.

**If you add a second consumer, move release to a `ProcessLifecycleOwner` observer and take
the dependency then.** Do not leave two ViewModels calling `shutdown()` on a shared
singleton.

## Narration

The domain may not read Android resources, and project convention forbids hardcoded Spanish
in Kotlin. Those two rules collide, so narration is split:

```
SummarizeMonthUseCase   (pure)     transactions + categories ──► MonthSummary
MonthSummaryNarrator    (domain interface)   MonthSummary ──► String
  └─ ResourceMonthSummaryNarrator (data)     reads strings.xml
SpeakMonthSummaryUseCase           narrator ──► SpeechSynthesizer
```

`MonthSummary` is a pure value object — the month, the expense total, an optional
month-over-month comparison, and an optional top category. No strings, no formatting, no
locale.

Amounts are **not** spoken as `MoneyFormat.format` output. `"$1.842.500"` is built for the
eye; a Spanish engine reads the `$` and the dot grouping as literal tokens or as decimals.
`MoneyFormat.spokenPesos()` emits bare digits (`"1842500"`), which the engine renders as
*"un millón ochocientos cuarenta y dos mil quinientos"*, and the string resource supplies
the word "pesos".

The narration always describes the **selected month's expenses**. It deliberately ignores
the Período and Gastos/Ingresos toggles, so there is one template rather than eight.

## Migrating to a cloud provider

The seam is one line. Implement the same interface and rebind it:

```kotlin
@Binds
abstract fun bindSpeechSynthesizer(impl: CloudSpeechSynthesizer): SpeechSynthesizer
```

Nothing in `core/domain`, `feature/dashboard`, or the tests changes — the tests already run
against a fake implementation of the same interface, so they are provider-agnostic by
construction.

What a cloud implementation has to honour:

- `speak` still suspends until playback finishes, not until the HTTP response arrives. The
  network fetch belongs inside `Preparing`, which is exactly what that state is for.
- `onStarted` fires when audio begins, not when bytes arrive.
- Cancellation must abort both the in-flight request and playback.
- The four `SpeechFailure` cases still need a mapping. Network and auth errors have no
  natural home in the current enum — add cases (e.g. `NETWORK_UNAVAILABLE`,
  `PROVIDER_REJECTED`) and let the exhaustive `when` in the ViewModel force the new strings.
- Credentials must not ship in the client. Route through a backend proxy; that is the main
  reason this sprint stopped at the on-device engine.

An LLM-generated narration is a *separate* swap, at `MonthSummaryNarrator`. The two are
independent on purpose: you can move to cloud speech while keeping the template, or
generate the sentence with a model while still speaking it on-device.

## Testing

Unit tests run against a fake `SpeechSynthesizer` on the JVM — plain JUnit4 and
`kotlinx-coroutines-test`, no Robolectric, no new dependencies. The fake suspends on a
`CompletableDeferred` the test controls, which makes every state observable, including the
race-prone ones: double-tap during `Preparing`, stop during `Speaking`, and each failure
mapping to its own string resource. `onCleared()` releasing the engine is asserted too.

What unit tests **cannot** cover is the engine itself: real init timing, voice-data
availability, and audio focus need a device or emulator. The missing-voice-data path in
particular is worth provoking deliberately on an image without Spanish TTS data, because it
is the failure most likely to be wrong in the wild.
