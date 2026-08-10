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

  /**
   * Whether to preserve thinking/reasoning content in the response. When enabled, the model will
   * include reasoning process in the output.
   */
  @SerializedName("preserve_thinking")
  private Boolean preserveThinking;

  /**
   * Controls the reasoning effort level for models that support it. Possible values: "low",
   * "medium", "high"
   */
  @SerializedName("reasoning_effort")
  private String reasoningEffort;

  /**
   * The maximum number of tokens to generate for completion. This is an alternative to max_tokens
   * following OpenAI's newer API convention.
   */
  @SerializedName("max_completion_tokens")
  private Integer maxCompletionTokens;

  /**
   * Whether to stream tool calls as they are generated. When true, tool calls will be sent
   * incrementally rather than all at once.
   */
  @SerializedName("tool_stream")
  private Boolean toolStream;

  /**
   * Enable high resolution image processing for vision-language models. Improves image
   * understanding quality at the cost of more tokens.
   */
  @SerializedName("vl_high_resolution_images")
  private Boolean vlHighResolutionImages;

  /** Enable hardware-accelerated image output for vision-language models. */
  @SerializedName("vl_enable_image_hw_output")
  private Boolean vlEnableImageHwOutput;

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
    if (preserveThinking != null) {
      requestObject.addProperty("preserve_thinking", preserveThinking);
    }
    if (reasoningEffort != null) {
      requestObject.addProperty("reasoning_effort", reasoningEffort);
    }
    if (maxCompletionTokens != null) {
      requestObject.addProperty("max_completion_tokens", maxCompletionTokens);
    }
    if (toolStream != null) {
      requestObject.addProperty("tool_stream", toolStream);
    }
    if (vlHighResolutionImages != null) {
      requestObject.addProperty("vl_high_resolution_images", vlHighResolutionImages);
    }
    if (vlEnableImageHwOutput != null) {
      requestObject.addProperty("vl_enable_image_hw_output", vlEnableImageHwOutput);
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
