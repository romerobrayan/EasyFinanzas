// JNI bridge for whisper.cpp. Deliberately thin: marshalling only, no logic.
// Everything here is called from a single thread — see WhisperSpeechRecognizer,
// which confines native calls to a single-threaded dispatcher because
// whisper_context is not thread-safe.

#include <jni.h>

#include <string>
#include <vector>

#include <android/log.h>

#include "whisper.h"

#define LOG_TAG "TintoWhisper"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// whisper logs every tensor at info level; keep it out of the app's logcat.
void quiet_log(ggml_log_level level, const char *text, void * /*user_data*/) {
    if (level == GGML_LOG_LEVEL_ERROR) {
        LOGE("%s", text);
    }
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_dev_romerobrayan_tinto_core_data_speech_WhisperNative_initContext(
        JNIEnv *env, jobject /* thiz */, jstring model_path) {
    whisper_log_set(quiet_log, nullptr);

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) {
        return 0;
    }

    whisper_context_params params = whisper_context_default_params();
    // No GPU backend is built for Android here; asking for one would only add a
    // failed probe on every load.
    params.use_gpu = false;

    whisper_context *ctx = whisper_init_from_file_with_params(path, params);
    env->ReleaseStringUTFChars(model_path, path);

    if (ctx == nullptr) {
        LOGE("whisper_init_from_file_with_params returned null");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_romerobrayan_tinto_core_data_speech_WhisperNative_freeContext(
        JNIEnv * /* env */, jobject /* thiz */, jlong handle) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (ctx != nullptr) {
        whisper_free(ctx);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_romerobrayan_tinto_core_data_speech_WhisperNative_transcribe(
        JNIEnv *env, jobject /* thiz */, jlong handle, jfloatArray audio, jint threads) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (ctx == nullptr || audio == nullptr) {
        return nullptr;
    }

    const jsize sample_count = env->GetArrayLength(audio);
    if (sample_count <= 0) {
        return nullptr;
    }

    // Copied rather than pinned: GetFloatArrayCritical would hold the GC for the
    // whole inference, which is seconds long.
    std::vector<float> samples(static_cast<size_t>(sample_count));
    env->GetFloatArrayRegion(audio, 0, sample_count, samples.data());

    // Beam search, not greedy: on a q5-quantized model greedy compounds every
    // slightly-wrong token, and the accuracy gap on short Spanish clips is large.
    // The cost is inference time, which the UI already models as Transcribing.
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_BEAM_SEARCH);
    params.beam_search.beam_size = 5;
    params.greedy.best_of        = 5;  // used by the temperature-fallback path
    // Pinned to Spanish rather than auto-detected: we know the user's language,
    // and detection costs an extra encoder pass and can pick wrong on short clips.
    params.language        = "es";
    params.translate       = false;
    params.n_threads       = threads;
    params.no_context      = true;
    params.suppress_blank  = true;
    // Whisper conditions on this as if it were preceding speech. Biasing the
    // decoder toward Colombian-Spanish expense phrasing with spelled-out number
    // words is the cheapest accuracy lever there is for this exact payload.
    params.initial_prompt =
        "Estoy registrando mis gastos en pesos colombianos. "
        "Gasté veinte mil pesos en el almuerzo, pagué cincuenta y cinco mil "
        "en el mercado y treinta mil de transporte.";
    params.print_progress  = false;
    params.print_realtime  = false;
    params.print_timestamps = false;
    params.print_special   = false;

    if (whisper_full(ctx, params, samples.data(), sample_count) != 0) {
        LOGE("whisper_full failed");
        return nullptr;
    }

    std::string transcript;
    const int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        if (text != nullptr) {
            transcript += text;
        }
    }

    return env->NewStringUTF(transcript.c_str());
}
