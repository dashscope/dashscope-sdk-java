package com.alibaba.dashscope.legacy_2x.audio.asr.phrase;

import com.alibaba.dashscope.legacy_2x.common.OutputMode;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.protocol.HttpMethod;
import com.alibaba.dashscope.legacy_2x.protocol.Protocol;
import com.alibaba.dashscope.legacy_2x.protocol.ServiceOption;
import com.alibaba.dashscope.legacy_2x.protocol.StreamingMode;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class AsrPhraseFinetuneOption implements ServiceOption {
  // set websocket service stream mode[NONE, IN, OUT, DUPLEX]
  @Builder.Default private StreamingMode streamingMode = StreamingMode.NONE;
  // set stream result output mode[accumulate, divide]
  // accumulate: Subsequent output contains previous output.
  // divide: Outputs are independent of each other.
  @Builder.Default private OutputMode outputMode = OutputMode.ACCUMULATE;
  // set communication protocol
  @Builder.Default private Protocol protocol = Protocol.HTTP;
  // if HTTP, set HTTP method.
  @Builder.Default private HttpMethod httpMethod = HttpMethod.POST;
  // Set service task group
  private String taskGroup;
  // Set service task
  private String task;
  // Set service function
  private String function;
  // Set is asynchronous task, only for HTTP
  @Builder.Default private Boolean isAsyncTask = false;
  // Set is Server-Send-Event, only for HTTP
  @Builder.Default private Boolean isSSE = false;

  private AsrPhraseOperationType operationType;

  private String fineTunedOutput;

  /** The request base url */
  @Default private String baseHttpUrl = null;

  @Default private String baseWebSocketUrl = null;

  @Override
  public String httpUrl() {
    if (operationType == AsrPhraseOperationType.CREATE
        || operationType == AsrPhraseOperationType.UPDATE) {
      return "/fine-tunes";
    } else if (operationType == AsrPhraseOperationType.QUERY
        || operationType == AsrPhraseOperationType.DELETE) {
      if (fineTunedOutput == null || fineTunedOutput.isEmpty()) {
        throw new ApiException(new RuntimeException("fineTunedOutput is empty"));
      }
      return "/fine-tunes/outputs/" + fineTunedOutput;
    } else {
      return "/fine-tunes/outputs";
    }
  }

  @Override
  public String getModule() {
    return "audio";
  }
}
