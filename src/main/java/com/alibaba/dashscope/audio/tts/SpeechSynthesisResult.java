package com.alibaba.dashscope.audio.tts;

import com.alibaba.dashscope.audio.tts.timestamp.Sentence;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Result;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.Response;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Maps;
import java.nio.ByteBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class SpeechSynthesisResult extends Result {

  private Sentence timestamp;

  private ByteBuffer audioFrame;

  @Override
  public void fromResponse(Protocol protocol, Response response) throws ApiException {
    String message = response.getMessage();
    ByteBuffer binary = response.getBinary();
    if (binary != null) {
      this.setAudioFrame(binary);
    }
    if (StringUtils.isNotBlank(message)) {
      try {
        SpeechSynthesisMessagePayload payload =
            JsonUtils.fromJson(message, SpeechSynthesisMessagePayload.class);
        this.setUsage(payload.getUsage());
        this.setTimestamp(
            Sentence.from(
                payload.getOutput().getAsJsonObject(SpeechSynthesisApiKeywords.SENTENCE)));
      } catch (Exception ignored) {
      }
    }
    this.setHeaders(response.getHeaders() == null ? Maps.newHashMap() : response.getHeaders());
  }
}
