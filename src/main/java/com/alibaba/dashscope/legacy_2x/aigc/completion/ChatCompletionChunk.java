package com.alibaba.dashscope.legacy_2x.aigc.completion;

import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.tools.ToolCallBase;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class ChatCompletionChunk {
  private String id;
  private List<Choice> choices;

  @Data
  public class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    private Integer index;

    private Delta delta;
    private ChatCompletionLogProbabilities logprobs;
  }
  ;

  @Data
  public static class Delta {
    /** The role, can be `user` and `system` and `assistant` and 'tool'. */
    String role;

    /** The conversation content. */
    String content;

    /** For tool calls */
    @SerializedName("tool_calls")
    List<ToolCallBase> toolCalls;
  }

  private Integer created;
  private String model;

  @SerializedName("service_tier")
  private String serviceTier;

  @SerializedName("system_fingerprint")
  private String systemFingerprint;

  private String object = "chat.completion.chunk";

  private ChatCompletionUsage usage;

  public static ChatCompletionChunk fromDashScopeResult(DashScopeResult dashscopeResult) {
    return JsonUtils.fromJson((JsonObject) dashscopeResult.getOutput(), ChatCompletionChunk.class);
  }
}
