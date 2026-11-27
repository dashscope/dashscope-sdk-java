package com.alibaba.dashscope.legacy_2x.aigc.completion;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
@Deprecated
public class ChatCompletionLogProbabilities {
  List<LogProbabilityContent> content;

  @Data
  public class LogProbabilityContent {
    private String token;
    private Float logprob;
    private Byte[] bytes;

    @SerializedName("top_logprobs")
    private List<LogProbability> topLogprobs;
  }

  @Data
  public class LogProbability {
    private String token;
    private Float logprob;
    private Byte[] bytes;
  }
}
