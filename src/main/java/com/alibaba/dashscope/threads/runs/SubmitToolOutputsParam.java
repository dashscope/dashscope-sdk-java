package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class SubmitToolOutputsParam extends FlattenHalfDuplexParamBase {
  @Singular
  @SerializedName("tool_outputs")
  private List<ToolOutput> toolOutputs;

  @Default private Boolean stream = false;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.add("tool_outputs", JsonUtils.toJsonArray(toolOutputs));
    requestObject.addProperty("stream", stream);
    addExtraBody(requestObject);
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (toolOutputs == null || toolOutputs.isEmpty()) {
      throw new InputRequiredException("The tool output is required!");
    }
  }
}
