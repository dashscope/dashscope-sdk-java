package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.threads.messages.MessageParamBase;
import com.alibaba.dashscope.tools.ToolBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class RunParam extends FlattenHalfDuplexParamBase {
  @NonNull
  @SerializedName("assistant_id")
  protected String assistantId;

  protected String model;
  protected String instructions;

  @SerializedName("additional_instructions")
  protected String additionalInstructions;

  @Singular
  @SerializedName("additional_messages")
  protected List<MessageParamBase> additionalMessages;

  @Singular protected List<ToolBase> tools;
  /** Metadata */
  @SerializedName("metadata")
  @Default
  protected Map<String, String> metadata = null;

  protected Float temperature;

  @Default protected Boolean stream = false;

  @SerializedName("max_prompt_tokens")
  protected Integer maxPromptTokens;

  @SerializedName("max_completion_tokens")
  protected Integer maxCompletionTokens;

  @Data
  public static class TruncationStrategy {
    private String type;

    @SerializedName("last_messages")
    private Integer lastMessages;
  }

  @SerializedName("truncation_strategy")
  protected TruncationStrategy truncationStrategy;

  @SerializedName("tool_choice")
  protected Object toolChoice;

  /** only support json_object. */
  @SerializedName("response_format")
  @Default
  protected Object responseFormat = "json_object";

  @SerializedName("parallel_tool_calls")
  protected Boolean parallelToolCalls;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty("assistant_id", assistantId);
    requestObject.addProperty("stream", stream);
    if (model != null && !model.isEmpty()) {
      requestObject.addProperty("model", model);
    }
    if (instructions != null && !instructions.isEmpty()) {
      requestObject.addProperty("instructions", instructions);
    }
    if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
      requestObject.addProperty("additional_instructions", additionalInstructions);
    }
    if (additionalMessages != null && !additionalMessages.isEmpty()) {
      requestObject.add("additional_messages", JsonUtils.toJsonArray(additionalMessages));
    }
    if (tools != null && !tools.isEmpty()) {
      requestObject.add("tools", JsonUtils.toJsonArray(tools));
    }
    if (metadata != null && !metadata.isEmpty()) {
      requestObject.add("metadata", JsonUtils.toJsonObject(metadata));
    }
    if (temperature != null) {
      requestObject.addProperty("temperature", temperature);
    }
    if (maxPromptTokens != null) {
      requestObject.addProperty("max_prompt_tokens", maxPromptTokens);
    }
    if (maxCompletionTokens != null) {
      requestObject.addProperty("max_completion_tokens", maxCompletionTokens);
    }
    if (truncationStrategy != null) {
      requestObject.add("truncation_strategy", JsonUtils.toJsonObject(truncationStrategy));
    }
    if (toolChoice != null) {
      if (toolChoice instanceof String) {
        requestObject.addProperty("tool_choice", (String) toolChoice);
      } else {
        requestObject.add("tool_choice", JsonUtils.toJsonObject(toolChoice));
      }
    }
    if (responseFormat != null) {
      requestObject.addProperty("response_format", (String) responseFormat);
    }
    if (parallelToolCalls != null) {
      requestObject.addProperty("parallel_tool_calls", parallelToolCalls);
    }
    addExtraBody(requestObject);
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("The assistantId must be set");
    }
    if (responseFormat != null) {
      if (responseFormat instanceof String) {
        if (((String) responseFormat).equals("json_object")) {
          return;
        }
      }
    }
    throw new InputRequiredException("The response format only support json_object");
  }
}
