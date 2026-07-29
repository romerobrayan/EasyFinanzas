# Speech-to-text

Tinto can transcribe a spoken expense, fully offline, using whisper.cpp on the
device. This document describes the contract, the two lifecycles the UI drives,
the threading rules the native layer imposes, and the fallback path to the
platform recognizer.

This sprint covers **transcription only**: no parsing of the transcript into an
`ExpenseDraft`, no cloud fallback, and no streaming. The model choice and its
(unmeasured) reasoning live in `docs/stt-whisper-model-choice.md`.

## Why there is an interface at all

The same argument as `docs/tts.md`, pointing the other way. Without a domain
contract:

- `core/domain` would import `android.media` and, worse, a JNI object.
- Every consumer would re-implement "check the model, ask for the mic, capture,
  convert, infer".
- Swapping to the platform recognizer would mean touching every call site.

So the domain owns the contract, `core/data/speech` owns the microphone and the
native library, and Hilt joins them — mirroring how the app already treats SMS
and notification capture.

## The contract

```kotlin
// core/domain/repository/SpeechRecognizer.kt
interface SpeechRecognizer {
    val model: SpeechModel
    val modelState: StateFlow<SpeechModelState>
    suspend fun refreshModelState()
    suspend fun prepareModel()
    fun recognize(): Flow<RecognitionState>
    fun stopRecording()
    fun release()
}
```

**Two lifecycles, deliberately separate.** The model is a ~57 MB file fetched
once and kept; a recognition is one press-and-hold, seconds long. Folding the
download into `recognize()` would give a first-run user a "listening" indicator
that silently blocks for a minute on mobile data. Keeping them apart lets the
screen render download progress as the distinct thing it is.

**`stopRecording()` is not cancellation.** This is the one place where a second
method genuinely earns its keep:

| Gesture | Call | Meaning |
| --- | --- | --- |
| Release the button | `stopRecording()` | End capture, keep the audio, transcribe it |
| Navigate away, cancel the job | cancel the collection | Stop the mic, throw the audio away |

Collapsing them would force one to be expressed as the other, and whichever lost
would surprise someone. `stopRecording()` sets a flag the capture loop polls once
per buffer — tens of milliseconds, well inside human reaction time.

Failures are returned as states, never thrown:

```kotlin
// core/domain/model/RecognitionState.kt
enum class RecognitionFailure {
    MODEL_UNAVAILABLE,       // not downloaded, or the .so is missing for this ABI
    MICROPHONE_UNAVAILABLE,  // permission missing, or the mic is busy
    RECORDING_FAILED,        // capture produced nothing usable
    TRANSCRIPTION_FAILED,    // the native call failed
    NO_SPEECH_DETECTED,      // audio captured, but whisper heard nothing
}
```

An enum, not free text, so the ViewModel maps each case to its own `@StringRes`
in an exhaustive `when`. Adding a case later is a compile error rather than a
silent fallthrough.

## There is no partial transcript

Whisper is batch by nature. Its encoder consumes a fixed, zero-padded
**30-second** log-mel window in a single pass; there is no intermediate result to
report. `RecognitionState` therefore has no `Partial` case, and the UI does not
pretend otherwise.

This has a useful consequence: a 5-second utterance and a 15-second one cost
roughly the same inference time. Clip length is not a latency lever. The
15-second cap exists to bound memory and to stop a stuck button recording
forever — not to make transcription faster.

## States

```
                    hold
    Idle ────────────────────────► Recording
      ▲                               │  release → stopRecording()
      │                               ▼
      │      Success(text)        Transcribing
      │  ┌────────────────────────────┤
      │  ▼                            │
    Transcribed  ◄───── editable      │
      │                               │
      │         Error(reason)         │
      └───────────────────────────────┘
              (a new hold clears it)
```

Recording and the model download are orthogonal; the screen renders whichever
precondition is unmet, in this order:

| Condition | What the screen shows |
| --- | --- |
| Permission permanently denied | Explainer + a button to system settings |
| Permission not granted | Explainer + the runtime prompt |
| Model not downloaded / failed | Size, a download button, progress, retry |
| Ready | The hold-to-record button |

## Permission as state

The task-level rule was that `RECORD_AUDIO` is modelled as ViewModel state, not
as an exception. `VoiceEntryUiState` carries a `MicPermission` with four cases,
and `DENIED` is kept distinct from `PERMANENTLY_DENIED` on purpose: one can be
re-asked, the other can only be sent to system settings. Collapsing them yields
a dialog whose button does nothing.

The screen still owns the `rememberLauncherForActivityResult` — the runtime
prompt needs an Activity, the same documented exception already made in
`ProfileScreen` and `RemindersScreen` — but it reports the outcome *into* the
ViewModel rather than branching on it. A `LifecycleResumeEffect` re-checks the
grant on resume, so revoking it in system settings lands without a restart.

## Threading

`whisper_context` **is not thread-safe**, and the microphone read loop blocks.
Both constraints are handled by where work runs, not by hoping:

| Stage | Thread |
| --- | --- |
| `AudioRecord` read loop | `dispatchers.io`, inside `channelFlow` (via `flowOn`) |
| PCM16 → normalized float | same loop, pure arithmetic |
| Native inference | `dispatchers.io.limitedParallelism(1, "whisper")` |
| State collection | the ViewModel, `collectAsStateWithLifecycle` |

The single-threaded dispatcher **is** the lock. There is no separate mutex to
forget to take, and `release()` is launched onto the same dispatcher — so freeing
the context queues behind any inference already running rather than pulling
memory out from under it.

The main thread does none of this.

Cancelling the collection propagates down: the capture loop checks
`ensureActive()` each buffer and the recorder's `finally` releases `AudioRecord`.
An inference already in flight is not aborted — for a clip of at most 15 s the
worst case is one wasted result, which is cheaper than wiring whisper's abort
callback for a case the user cannot observe.

## Model lifecycle

Models are **not bundled in the APK**. A 57 MB asset would land on every user
including those who never dictate anything.

`SpeechModelStore` runs the fetch over plain `HttpURLConnection` — the app has no
HTTP client and one large GET does not justify adding one.

The integrity rules are strict because the failure they prevent is silent:

- Bytes stream into `<id>.part` and are SHA-256 hashed **as they arrive**, so the
  happy path never re-reads 57 MB.
- The file is renamed into place only after the digest matches. **A file at the
  final path is therefore always verified** — "exists" means "trustworthy", which
  is what lets `resolve()` be a cheap existence check.
- A mismatch deletes the partial file. A corrupt download must never become a
  permanent bad cache that every retry resumes into.
- Retries resume with a `Range` header; the existing prefix is re-hashed first so
  the digest still covers the whole file. A server that ignores `Range` and
  answers `200` restarts the transfer cleanly.
- Files live in `filesDir`, not `cacheDir`. The OS must not evict something the
  user waited minutes on mobile data for.

**We fail closed.** A model whose `SpeechModel.sha256` is not a full-length
digest is refused with `CHECKSUM_UNKNOWN` rather than downloaded unverified. The
two shipped models carry the digests published on their HuggingFace file pages;
the guard stays because it protects the *next* model someone adds before its
digest is known. If upstream ever re-uploads a model file, downloads start
failing with `CHECKSUM_MISMATCH` — that is the alarm working, not a bug.

## Native build

whisper.cpp is fetched at CMake configure time rather than vendored, so we do not
carry ~100k lines of third-party C++ in our history. The fetch is pinned to a
full commit SHA (the commit tag `v1.7.4` points at), not the tag name — tags are
mutable, and the native code in a build of a finance app must not be able to
change underneath us. Upgrading is: pick the new tag, `git ls-remote` its
commit, replace the hash in `app/src/main/cpp/CMakeLists.txt`.

Both ABIs are built:

| ABI | Notes |
| --- | --- |
| `arm64-v8a` | The target. Gets `base-q5_1`. |
| `armeabi-v7a` | Builds with NEON, no OpenMP, no BLAS. Gets `tiny-q5_1` — `base` is too slow to be a product on 32-bit hardware. |

`ndkVersion` is pinned to r28+ because Android 15 introduced 16 KB memory pages
and r28 aligns to them by default; `-Wl,-z,max-page-size=16384` is passed
explicitly as well, so an older NDK cannot silently produce a library that fails
to load on newer hardware.

The JNI surface is four calls (`initContext`, `freeContext`, `transcribe`, plus
the `isAvailable` library-load probe). `isAvailable` turns a missing `.so` into a
`MODEL_UNAVAILABLE` state instead of an `UnsatisfiedLinkError` at an arbitrary
call site.

Language is pinned to `es` rather than auto-detected: we know the user's
language, and detection costs an extra pass and can pick wrong on short clips.

## Falling back to the platform recognizer

If on-device Whisper has to go — the APK grows too much, the latency proves
unacceptable on real hardware, or the accuracy on Colombian numerals turns out to
be inadequate — the seam is one line:

```kotlin
@Binds
abstract fun bindSpeechRecognizer(impl: PlatformSpeechRecognizer): SpeechRecognizer
```

Nothing in `core/domain`, `feature/voiceentry`, or the tests changes; the tests
already run against a fake of the same interface.

What an `android.speech.SpeechRecognizer` implementation has to honour:

- `modelState` becomes trivially `Ready`, and `prepareModel()` a no-op. The UI
  already handles `Ready` as the uninteresting case, so the download panel simply
  never renders.
- `recognize()` still emits exactly one terminal state. The platform API delivers
  results through `RecognitionListener`; bridge it with `callbackFlow` and let
  `awaitClose` call `destroy()`.
- `stopRecording()` maps to `SpeechRecognizer.stopListening()` — which happens to
  mean the same thing, and is part of why the contract is shaped this way.
- The failure enum needs mapping, and it does not cover everything the platform
  can report. `ERROR_NETWORK` and `ERROR_SERVER` have no home in the current
  cases — add them and let the exhaustive `when` force the new strings.
- **The privacy story changes.** The platform recognizer may send audio to a
  server depending on the device and whether `EXTRA_PREFER_OFFLINE` is honoured.
  The current copy tells the user their audio never leaves the phone. If that
  stops being true, the copy has to change with it — this is the main reason the
  fallback is documented rather than shipped.

## Testing

JVM unit tests run against a fake `SpeechRecognizer` — plain JUnit4 and
`kotlinx-coroutines-test`, no Robolectric, no mocking framework, no new
dependencies. The fake pushes recognition states through a channel the test
controls, which makes every intermediate phase observable: the precondition gate
(neither permission nor model alone unlocks recording), the difference between
release and cancel, the transcript staying editable, and each failure mapping to
its own string resource.

The download flow gets an **instrumented** test instead, because it needs a real
filesystem and a real `HttpURLConnection`. It runs against a hand-rolled
`ServerSocket` that understands `Range`, and pins the integrity properties:
verified-or-nothing at the final path, a corrupt download deleted rather than
cached, resume continuing instead of restarting, and an unverifiable model
refused without a single request.

What no test here covers is **the model itself** — real inference latency,
memory, and Spanish accuracy on Colombian number words need a device and real
audio. That gap is the subject of `docs/stt-whisper-model-choice.md`, and it is
the reason the model default is a value in one file rather than an assumption
spread through the layer.
