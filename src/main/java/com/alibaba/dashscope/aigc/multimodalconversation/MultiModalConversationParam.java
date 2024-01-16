package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class MultiModalConversationParam extends HalfDuplexServiceParam {

  @Singular private List<Object> messages;
  /* The maximum length of tokens to generate.
  The token count of your prompt plus max_length
  cannot exceed the model's context length. Most models
  have a context length of 2000 tokens */
  private Integer maxLength;
  /* A sampling strategy, called nucleus
  sampling, where the model considers the results of the
  tokens with top_p probability mass. So 0.1 means only
  the tokens comprising the top 10% probability mass are
  considered */
  private Double topP;

  /* A sampling strategy, the k largest elements of the
  given mass are  considered */
  private Integer topK;
  /* Whether to enable web search(quark).
  Currently works best only on the first round of conversation.
  Default to False */
  @Builder.Default private Boolean enableSearch = false;
  /*
   * When generating, the seed of the random number is used to control the randomness of the model generation.
   * If you use the same seed, each run will generate the same results;
   * you can use the same seed when you need to reproduce the model's generated results.
   * The seed parameter supports unsigned 64-bit integer types. Default value 1234
   */
  private Integer seed;

  /**
   * Used to control the degree of randomness and diversity. Specifically, the temperature value
   * controls the degree to which the probability distribution of each candidate word is smoothed
   * when generating text. A higher temperature value will reduce the peak value of the probability
   * distribution, allowing more low-probability words to be selected, and the generated results
   * will be more diverse; while a lower temperature value will enhance the peak value of the
   * probability distribution, making it easier for high-probability words to be selected, the
   * generated results are more deterministic, range(0, 2).
   */
  private Float temperature;

  /**
   * Used to control the streaming output mode. If true, the subsequent output will include the
   * previously input content by default. Otherwise, the subsequent output will not include the
   * previously output content. Default: false eg(false):
   *
   * <pre>
   * I
   * I like
   * I like apple
   * when true:
   * I
   * like
   * apple
   * </pre>
   */
  @Builder.Default private Boolean incrementalOutput = false;

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
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.add(ApiKeywords.MESSAGES, JsonUtils.toJsonArray(messages));
    return jsonObject;
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (maxLength != null) {
      params.put("max_length", maxLength);
    }
    if (topP != null) {
      params.put("top_p", topP);
    }
    if (topK != null) {
      params.put("top_k", topK);
    }
    params.put("enable_search", enableSearch);
    if (seed != null) {
      params.put("seed", seed);
    }
    if (temperature != null) {
      params.put("temperature", temperature);
    }
    if (incrementalOutput) {
      params.put("incremental_output", incrementalOutput);
    }
    params.putAll(parameters);
    return params;
  }

  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (messages == null || messages.isEmpty()) {
      throw new InputRequiredException("Message must not null or empty!");
    }
  }
}
