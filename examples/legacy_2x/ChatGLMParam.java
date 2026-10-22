// Copyright (c) Alibaba, Inc. and its affiliates.
import static com.alibaba.dashscope.utils.ApiKeywords.HISTORY;
import static com.alibaba.dashscope.utils.ApiKeywords.PROMPT;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ChatGLMParam extends HalfDuplexServiceParam {
  /** The input prompt. */
  private String prompt;

  /** { "user":"今天天气好吗？", "bot":"今天天气不错，要出去玩玩嘛？" }, */
  private List<List<String>> history;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(PROMPT, getPrompt());
    JsonArray ar = JsonUtils.toJsonElement(history).getAsJsonArray();
    jsonObject.add(HISTORY, ar);
    return jsonObject;
  }

  /**
   * Get the websocket binary data, only for websocket binary input data.
   *
   * @return Generation param has no binary data.
   */
  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    requestObject.add(ApiKeywords.INPUT, getInput());
    Map<String, Object> params = getParameters();
    if (params != null && !params.isEmpty()) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(params));
    }
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {}
}
