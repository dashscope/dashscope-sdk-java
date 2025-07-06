package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.tools.ToolBase;
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

  /**
   * The maximum length of tokens to generate. The token count of your prompt plus max_length cannot
   * exceed the model's context length. Most models have a context length of 2000 tokens
   */
  @Deprecated private Integer maxLength;
  /**
   * A sampling strategy, called nucleus sampling, where the model considers the results of the
   * tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10%
   * probability mass are considered
   */
  private Double topP;

  /** A sampling strategy, the k largest elements of the given mass are considered */
  private Integer topK;

  /**
   * Used to control the repetitiveness of the model when generating text. Increasing the
   * repetition_penalty can reduce the repetitiveness of the model's output. A value of 1.0
   * indicates no penalty. Default value: 1.0
   */
  private Float repetitionPenalty;

  /**
   * A parameter that controls the repetitiveness of words. It reduces the probability of the same
   * word appearing repeatedly by penalizing words that have already been generated, thus increasing
   * the diversity of the generated text. A value of 0 indicates no penalty. Default value: 0.0
   */
  private Float presencePenalty;

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
   * generated results are more deterministic, range(0, 2). Default value: 1.0
   */
  private Float temperature;

  /**
   * The maximum length of tokens to generate. The token count of your prompt plus max_length cannot
   * exceed the model's context length.
   */
  private Integer maxTokens;

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

  /** Output format of the model including "text" and "audio". Default value: ["text"] */
  private List<String> modalities;

  /** audio output parameters */
  private AudioParameters audio;

  /** ocr options */
  private OcrOptions ocrOptions;

  /** text input */
  private String text;

  /** voice of tts */
  private AudioParameters.Voice voice;

  /** Specify which tools the model can use. */
  private List<ToolBase> tools;

  /** Specify tool choice */
  protected Object toolChoice;

  /** enable parallel tool calls */
  protected Boolean parallelToolCalls;

  /** enable vl_high_resolution_images */
  protected Boolean vlHighResolutionImages;

  /** enable vl_enable_image_hw_output */
  protected Boolean vlEnableImageHwOutput;

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

    if (text != null) {
      jsonObject.addProperty(ApiKeywords.TEXT, text);
    }

    if (voice != null) {
      jsonObject.addProperty(ApiKeywords.VOICE, voice.getValue());
    }

    return jsonObject;
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (maxLength != null) {
      params.put(ApiKeywords.MAX_LENGTH, maxLength);
    }

    if (maxTokens != null) {
      params.put(ApiKeywords.MAX_TOKENS, maxTokens);
    }

    if (topP != null) {
      params.put(ApiKeywords.TOP_P, topP);
    }

    if (topK != null) {
      params.put(ApiKeywords.TOP_K, topK);
    }

    params.put("enable_search", enableSearch);

    if (seed != null) {
      params.put(ApiKeywords.SEED, seed);
    }

    if (temperature != null) {
      params.put(ApiKeywords.TEMPERATURE, temperature);
    }

    if (repetitionPenalty != null) {
      params.put(ApiKeywords.REPETITION_PENALTY, repetitionPenalty);
    }

    if (presencePenalty != null) {
      params.put(ApiKeywords.PRESENCE_PENALTY, presencePenalty);
    }

    if (incrementalOutput) {
      params.put(ApiKeywords.INCREMENTAL_OUTPUT, incrementalOutput);
    }

    if (modalities != null) {
      params.put(ApiKeywords.MODALITIES, modalities);
    }

    if (audio != null) {
      params.put(ApiKeywords.AUDIO, audio);
    }

    if (ocrOptions != null) {
      params.put(ApiKeywords.OCR_OPTIONS, ocrOptions);
    }

    if (tools != null && !tools.isEmpty()) {
      params.put("tools", tools);
    }

    if (toolChoice != null) {
      if (toolChoice instanceof String) {
        params.put("tool_choice", (String) toolChoice);
      } else {
        params.put("tool_choice", JsonUtils.toJsonObject(toolChoice));
      }
    }

    if (parallelToolCalls != null) {
      params.put("parallel_tool_calls", parallelToolCalls);
    }

    if (vlHighResolutionImages != null) {
      params.put("vl_high_resolution_images", vlHighResolutionImages);
    }

    if (vlEnableImageHwOutput != null) {
      params.put("vl_enable_image_hw_output", vlEnableImageHwOutput);
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
    if (messages == null || messages.isEmpty() && (text == null || text.isEmpty())) {
      throw new InputRequiredException("Message or text must not null or empty!");
    }
  }
}
