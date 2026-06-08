package com.alibaba.dashscope.aigc.completion;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class ChatCompletion {
  private String id;
  private List<Choice> choices;

  @Data
  public class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    private Integer index;

    private Message message;
    private ChatCompletionLogProbabilities logprobs;
  }

  private Integer created;
  private String model;

  @SerializedName("service_tier")
  private String serviceTier;

  @SerializedName("system_fingerprint")
  private String systemFingerprint;

  private String object = "chat.completion";

  private ChatCompletionUsage usage;

  public static ChatCompletion fromDashScopeResult(DashScopeResult result) {
    return JsonUtils.fromJson((JsonObject) result.getOutput(), ChatCompletion.class);
  }
}
