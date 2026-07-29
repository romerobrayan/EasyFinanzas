# Phase 0 — Whisper model choice

Which ggml model Tinto should ship for on-device Spanish speech-to-text, and
whether the latency is acceptable.

## Read this first: these numbers are not measured

The brief asked for a spike on real hardware — five Colombian-accented clips,
both models, measured cold-start, transcription time, peak memory, and
word-level accuracy on amounts and categories. **That spike was not run.** The
build environment has no Android SDK, no NDK, no `adb`, and no device or
emulator, and both `huggingface.co` and `github.com` codeload are blocked by
network policy, so neither the models nor whisper.cpp itself could be fetched.

What follows is reasoning from published whisper/whisper.cpp characteristics and
the architecture of the model. Every timing below is an **indicative range for
comparable ARM hardware, not a measurement of Tinto**. Treat them as a basis for
a provisional decision, not as evidence.

One consequence is worth stating up front: the accuracy question — the one that
actually decides this — **cannot be answered from published data at all**. See
"The accuracy gap".

## The models

| | `ggml-tiny-q5_1` | `ggml-base-q5_1` |
| --- | --- | --- |
| Parameters | 39 M | 74 M |
| File size (download) | ~31 MB | ~57 MB |
| Cold model load (mmap, flash) | ~0.2–0.5 s | ~0.5–1.5 s |
| Transcription, mid-range arm64 | ~1–3 s | ~3–8 s |
| Transcription, armeabi-v7a | ~5–12 s | ~15 s+ |
| Peak RSS during inference | ~120–150 MB | ~200–280 MB |
| Spanish quality | Weak | Usable |

Ranges are wide because SoC variation dominates. A 2024 mid-range Snapdragon
and a 2019 budget one differ by more than the gap between the two models.

## Clip length is not the lever

Worth understanding before optimising the wrong thing: **Whisper always encodes
a padded 30-second window.** The encoder input is a fixed-size log-mel
spectrogram, so a 5-second utterance and a 15-second one cost the same encoder
pass. Only the decoder scales with output length, and for a short expense
phrase the output is a handful of tokens.

So the 15-second recording cap does not cost 3× a 5-second one — it costs
almost nothing extra. And trimming utterances shorter buys nothing. The only
real levers are **model size** and **thread count**.

## Does base exceed ~4 s on a mid-range device?

**Probably yes — plan for 3–8 s, and treat 4 s as optimistic.** That is the
plain answer the brief asked for. On armeabi-v7a, base is not merely slow, it is
unusable.

Two things soften it, and one does not:

- The wait is **after** the user releases the button. It is a single bounded
  wait behind an explicit `Transcribing` state, not a stall during interaction.
- It is **fully offline**, so it is a predictable 3–8 s rather than a network
  round-trip that varies with signal.
- It does **not** soften on armeabi-v7a. There, base is out.

## The accuracy gap

Published WER figures (Common Voice, FLEURS) show a large tiny→base jump for
Spanish — roughly 20%+ down to the low teens, dataset-dependent, with q5_1
quantization adding a small further loss. But those benchmarks measure general
transcription of read speech, and **they do not tell us the thing that matters
here**: how either model handles Colombian-accented *number words*.

That is the entire payload. `"gasté veinte mil pesos en almuerzo"` is a success
only if `veinte mil` survives. A transcript that nails "almuerzo" and turns
"veinte mil" into "20" or "veintemil" or drops it is not 90% correct — it is
useless, and worse than useless if the user doesn't notice. Whisper tiny is
known to mangle multi-word numerals; how badly, in this accent, is exactly what
the five clips would have shown.

**No published benchmark can close this.** It needs the clips.

## Recommendation

Ship **`ggml-base-q5_1` as the default on `arm64-v8a`**, and
**`ggml-tiny-q5_1` on `armeabi-v7a`**.

The reasoning is that the failure modes are not symmetric. Being slow is
visible, bounded, and the user can see it happening. Being wrong about an amount
is silent and corrupts the ledger. Paying 3–8 s for a number you can trust beats
1–3 s for one you have to re-read and retype — and re-typing was the thing this
feature existed to avoid.

On armeabi-v7a there is no such trade to make: base is too slow to be a product,
so tiny is what runs, with correspondingly lower expectations.

**This is provisional.** If the five clips show tiny handling Colombian numerals
adequately, tiny becomes the default everywhere and the latency problem
disappears. If they show *base* struggling too, the honest conclusion is that
`small-q5_1` (~181 MB, slower still) is the floor for this use case — and that
would be a real product decision about whether on-device STT is viable at all,
not a tuning exercise.

So the architecture treats the model identity as **data, not a constant**
(`core/domain/model/SpeechModel.kt`): id, URL, expected SHA-256, size. Changing
the default is editing one value, not rewriting a layer. That is deliberate,
because the decision above is the one most likely to change once real audio
exists.

## First field result (2026-07-29)

The first real-device test — one user, Colombian Spanish, `base-q5_1` on
arm64 — produced hallucinated, out-of-context transcripts on the reference
phrase. That output pattern points at the audio/decoding pipeline (silence or
too-quiet capture, greedy decoding) rather than purely at model capacity, and
three fixes shipped in response: a press/release race that could record 15 s of
silence, peak normalization before inference, and beam search plus a
domain-biased initial prompt. The model-quality verdict is therefore **still
open** — it needs a retest on the fixed pipeline before concluding that
`small-q5_1` is the floor.

## What would settle it

Five clips, both models, comparing transcripts word-for-word on the amount and
the category noun. That test remains worth running on the first device build —
it is cheap once the JNI bridge exists, and it is the only thing that turns this
document from reasoning into evidence.
