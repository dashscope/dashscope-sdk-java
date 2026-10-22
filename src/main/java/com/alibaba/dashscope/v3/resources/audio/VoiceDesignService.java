// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.types.audio.DesignedVoice;
import com.alibaba.dashscope.v3.types.audio.DesignedVoiceList;
import com.alibaba.dashscope.v3.types.audio.VoiceDesignCreateParams;
import com.alibaba.dashscope.v3.types.common.JobStatus;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code audio.voiceDesign} — qwen voice design (声音设计; audio design §1.4). Shares the tts
 * customization endpoint with shorter action words and a {@code voice} id field instead of
 * {@code voice_id}; there is no {@code update} verb on this family.
 */
public final class VoiceDesignService {
  static final String PATH = "/services/audio/tts/customization";
  static final String DEFAULT_MODEL = "qwen-voice-design";

  private final HttpTransport transport;

  public VoiceDesignService(HttpTransport transport) {
    this.transport = transport;
  }

  /** Design a voice from a text prompt; the result carries {@code voice} + preview. */
  public DesignedVoice create(VoiceDesignCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public DesignedVoice create(VoiceDesignCreateParams params, RequestOptions options) {
    Map<String, Object> fields = new LinkedHashMap<>();
    if (params.getExtraInput() != null) {
      fields.putAll(params.getExtraInput());
    }
    // Named fields win over the extraInput escape hatch.
    fields.put("target_model", params.getTargetModel());
    fields.put("voice_prompt", params.getVoicePrompt());
    fields.put("preview_text", params.getPreviewText());
    if (params.getPreferredName() != null) {
      fields.put("preferred_name", params.getPreferredName());
    }
    if (params.getLanguage() != null) {
      fields.put("language", params.getLanguage());
    }
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(
                DEFAULT_MODEL, params.getModel(), "create", fields, params.getParameters()),
            CustomizationSupport.MODULE,
            options),
        DesignedVoice.class);
  }

  /** List designed voices; pages are 0-based on the wire. */
  public DesignedVoiceList list(int pageIndex, int pageSize) {
    return list(pageIndex, pageSize, null, RequestOptions.NONE);
  }

  public DesignedVoiceList list(int pageIndex, int pageSize, String model, RequestOptions options) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("page_index", pageIndex);
    fields.put("page_size", pageSize);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "list", fields, null),
            CustomizationSupport.MODULE,
            options),
        DesignedVoiceList.class);
  }

  /** Fetch one designed voice (wire action {@code query}). */
  public DesignedVoice get(String voice) {
    return get(voice, null, RequestOptions.NONE);
  }

  public DesignedVoice get(String voice, String model, RequestOptions options) {
    CustomizationSupport.requireId(voice, "voice");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("voice", voice);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "query", fields, null),
            CustomizationSupport.MODULE,
            options),
        DesignedVoice.class);
  }

  /** Delete one designed voice. */
  public JobStatus delete(String voice) {
    return delete(voice, null, RequestOptions.NONE);
  }

  public JobStatus delete(String voice, String model, RequestOptions options) {
    CustomizationSupport.requireId(voice, "voice");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("voice", voice);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "delete", fields, null),
            CustomizationSupport.MODULE,
            options),
        JobStatus.class);
  }
}
