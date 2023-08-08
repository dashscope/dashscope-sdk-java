// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.tts;

import com.alibaba.dashscope.audio.tts.timestamp.Sentence;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.nio.ByteBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode()
@Slf4j
public class SpeechSynthesisResult {
  @SerializedName(ApiKeywords.REQUEST_ID)
  private String requestId;
  /** The model outputs. */
  private JsonObject output;

  /** The data usage. */
  private SpeechSynthesisUsage usage;

  private Sentence timestamp;

  private ByteBuffer audioFrame;

  private static ByteBuffer cloneBuffer(ByteBuffer original) {
    ByteBuffer clone = ByteBuffer.allocate(original.capacity());
    original.rewind(); // copy from the beginning
    clone.put(original);
    original.rewind();
    clone.flip();
    return clone;
  }

  public static SpeechSynthesisResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    SpeechSynthesisResult result = new SpeechSynthesisResult();
    if (dashScopeResult.getOutput() instanceof ByteBuffer) {
      result.audioFrame = cloneBuffer((ByteBuffer) dashScopeResult.getOutput());
    }
    try {
      if (dashScopeResult.getRequestId() != null) {
        result.setRequestId(dashScopeResult.getRequestId());
      }
      if (dashScopeResult.getUsage() != null) {
        result.setUsage(
            JsonUtils.fromJsonObject(
                dashScopeResult.getUsage().getAsJsonObject(), SpeechSynthesisUsage.class));
      }
      if (dashScopeResult.getOutput() != null) {
        result.timestamp =
            Sentence.from(
                ((JsonObject) dashScopeResult.getOutput())
                    .getAsJsonObject(SpeechSynthesisApiKeywords.SENTENCE));
      }
    } catch (Exception ignored) {
    }
    return result;
  }
}
