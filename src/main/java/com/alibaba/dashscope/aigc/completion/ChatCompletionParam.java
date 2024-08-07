package com.alibaba.dashscope.aigc.completion;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.tools.ToolBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.reactivex.annotations.NonNull;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ChatCompletionParam extends FlattenHalfDuplexParamBase {
  @NonNull private List<Message> messages;
  @NonNull private String model;
  /**
   * Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing
   * frequency in the text so far, decreasing the model's likelihood to repeat the same line
   * verbatim.
   */
  @SerializedName("frequency_penalty")
  Float frequencyPenalty;

  @SerializedName("logit_bias")
  Map<Integer, Integer> logitBias;

  Boolean logprobs;

  @SerializedName("top_logprobs")
  Integer topLogprobs;

  @SerializedName("max_tokens")
  Integer maxTokens;

  Integer n;

  @SerializedName("presence_penalty")
  Float presencePenalty;

  @SerializedName("response_format")
  String responseFormat;

  Integer seed;

  @SerializedName("service_tier")
  String serviceTier;

  @Singular("stop")
  private List<String> stop;

  Boolean stream;

  @SerializedName("stream_options")
  private ChatCompletionStreamOptions streamOptions;

  private Float temperature;

  @SerializedName("top_p")
  private Integer topP;

  /*
   * Specify which tools the model can use.
   */
  private List<ToolBase> tools;

  /*
   * Specify tool choice
   */
  @SerializedName("tool_choice")
  protected Object toolChoice;

  @SerializedName("parallel_tool_calls")
  private Boolean parallelToolCalls;

  private String user;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty("model", model);
    requestObject.add("messages", JsonUtils.toJsonArray(messages));
    if (frequencyPenalty != null) {
      requestObject.addProperty("frequency_penalty", frequencyPenalty);
    }
    if (logitBias != null) {
      requestObject.add("logit_bias", JsonUtils.toJsonObject(logitBias));
    }
    if (logprobs != null) {
      requestObject.addProperty("logprobs", logprobs);
    }
    if (topLogprobs != null) {
      requestObject.addProperty("top_logprobs", topLogprobs);
    }
    if (maxTokens != null) {
      requestObject.addProperty("max_tokens", maxTokens);
    }
    if (n != null) {
      requestObject.addProperty("n", n);
    }
    if (presencePenalty != null) {
      requestObject.addProperty("presence_penalty", presencePenalty);
    }
    if (responseFormat != null) {
      requestObject.addProperty("response_format", responseFormat);
    }
    if (seed != null) {
      requestObject.addProperty("seed", seed);
    }
    if (serviceTier != null) {
      requestObject.addProperty("service_tier", serviceTier);
    }
    if (stop != null && !stop.isEmpty()) {
      requestObject.add("stop", JsonUtils.toJsonArray(stop));
    }
    if (stream != null) {
      requestObject.addProperty("stream", stream);
    }

    if (streamOptions != null) {
      requestObject.add("stream_options", JsonUtils.toJsonObject(streamOptions));
    }
    if (temperature != null) {
      requestObject.addProperty("temperature", temperature);
    }
    if (topP != null) {
      requestObject.addProperty("top_p", topP);
    }
    if (tools != null && !tools.isEmpty()) {
      requestObject.add("tools", JsonUtils.toJsonArray(tools));
    }
    if (toolChoice != null) {
      requestObject.add("tool_choice", JsonUtils.toJsonObject(toolChoice));
    }
    if (parallelToolCalls != null) {
      requestObject.addProperty("parallel_tool_calls", parallelToolCalls);
    }
    if (user != null) {
      requestObject.addProperty("user", user);
    }

    addExtraBody(requestObject);
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (model == null || messages.isEmpty()) {
      throw new InputRequiredException("The model and message must be set");
    }
  }
}
