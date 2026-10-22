// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.types.audio.Voice;
import com.alibaba.dashscope.types.audio.VoiceCreateParams;
import com.alibaba.dashscope.types.audio.VoiceList;
import com.alibaba.dashscope.types.common.JobStatus;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code audio.voices} — voice cloning (声音复刻; audio design §1.4, Python v2 customization
 * parity). All verbs POST the tts customization endpoint with an action word; {@code
 * update}/{@code delete} return {@link JobStatus}.
 */
public final class VoicesService {
  static final String PATH = "/services/audio/tts/customization";
  static final String DEFAULT_MODEL = "voice-enrollment";

  private final HttpTransport transport;

  public VoicesService(HttpTransport transport) {
    this.transport = transport;
  }

  /** Clone a voice from the audio at {@code url}; the result carries {@code voiceId}. */
  public Voice create(VoiceCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Voice create(VoiceCreateParams params, RequestOptions options) {
    Map<String, Object> fields = new LinkedHashMap<>();
    if (params.getExtraInput() != null) {
      fields.putAll(params.getExtraInput());
    }
    // Named fields win over the extraInput escape hatch.
    fields.put("target_model", params.getTargetModel());
    fields.put("prefix", params.getPrefix());
    fields.put("url", params.getUrl());
    if (params.getLanguageHints() != null) {
      fields.put("language_hints", params.getLanguageHints());
    }
    if (params.getMaxPromptAudioLength() != null) {
      fields.put("max_prompt_audio_length", params.getMaxPromptAudioLength());
    }
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(
                DEFAULT_MODEL, params.getModel(), "create_voice", fields, params.getParameters()),
            CustomizationSupport.MODULE,
            options),
        Voice.class);
  }

  /** List cloned voices, optionally narrowed to one {@code prefix}; pages are 0-based. */
  public VoiceList list(String prefix, int pageIndex, int pageSize) {
    return list(prefix, pageIndex, pageSize, null, RequestOptions.NONE);
  }

  public VoiceList list(
      String prefix, int pageIndex, int pageSize, String model, RequestOptions options) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("page_index", pageIndex);
    fields.put("page_size", pageSize);
    if (prefix != null && !prefix.isEmpty()) {
      fields.put("prefix", prefix);
    }
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "list_voice", fields, null),
            CustomizationSupport.MODULE,
            options),
        VoiceList.class);
  }

  /** Fetch one voice's details (1.x {@code query_voice}). */
  public Voice get(String voiceId) {
    return get(voiceId, null, RequestOptions.NONE);
  }

  public Voice get(String voiceId, String model, RequestOptions options) {
    CustomizationSupport.requireId(voiceId, "voice_id");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("voice_id", voiceId);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "query_voice", fields, null),
            CustomizationSupport.MODULE,
            options),
        Voice.class);
  }

  /** Re-clone one voice from the audio at {@code url}. */
  public JobStatus update(String voiceId, String url) {
    return update(voiceId, url, null, RequestOptions.NONE);
  }

  public JobStatus update(String voiceId, String url, String model, RequestOptions options) {
    CustomizationSupport.requireId(voiceId, "voice_id");
    CustomizationSupport.requireId(url, "url");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("voice_id", voiceId);
    fields.put("url", url);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "update_voice", fields, null),
            CustomizationSupport.MODULE,
            options),
        JobStatus.class);
  }

  /** Delete one cloned voice. */
  public JobStatus delete(String voiceId) {
    return delete(voiceId, null, RequestOptions.NONE);
  }

  public JobStatus delete(String voiceId, String model, RequestOptions options) {
    CustomizationSupport.requireId(voiceId, "voice_id");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("voice_id", voiceId);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "delete_voice", fields, null),
            CustomizationSupport.MODULE,
            options),
        JobStatus.class);
  }
}
