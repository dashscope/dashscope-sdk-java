package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.audio.asr.recognition.timestamp.Sentence;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Result;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.Response;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecognitionResult extends Result {
  private Sentence sentence = new Sentence();

  private boolean isCompleteResult = false;

  public boolean isSentenceEnd() {
    return sentence.getEndTime() != null;
  }

  public static boolean IsSentenceEnd(Sentence sentence) {
    return sentence != null && sentence.getEndTime() != null;
  }

  @Override
  public void fromResponse(Protocol protocol, Response response) throws ApiException {
    String message = response.getMessage();
    if (message != null) {
      RecognitionMessagePayload payload =
          JsonUtils.fromJson(message, RecognitionMessagePayload.class);
      this.setUsage(payload.getUsage());
      JsonObject output = payload.getOutput();
      if (output != null) {
        JsonObject timestampObject = output.getAsJsonObject(RecognitionApiKeywords.SENTENCE);
        if (timestampObject != null) {
          this.setSentence(Sentence.from(timestampObject));
        } else {
          isCompleteResult = true;
        }
      }
    }
    this.setHeaders(response.getHeaders() == null ? Maps.newHashMap() : response.getHeaders());
  }
}
