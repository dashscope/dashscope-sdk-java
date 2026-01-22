package com.alibaba.dashscope.aigc.imagegeneration;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.ResponseFormat;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.ParamUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ImageGenerationParam extends HalfDuplexServiceParam {

  @Singular private List<ImageGenerationMessage> messages;

  /*
   * When generating, the seed of the random number is used to control the randomness of the model generation.
   * If you use the same seed, each run will generate the same results;
   * you can use the same seed when you need to reproduce the model's generated results.
   * The seed parameter supports unsigned 64-bit integer types. Default value 1234
   */
  private Integer seed;

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
  @Builder.Default private Boolean incrementalOutput = null;

  /** Output format of the model including "text" and "audio". Default value: ["text"] */
  private List<String> modalities;

  /** response format */
  private ResponseFormat responseFormat;

  /** negative prompt */
  private String negativePrompt;

  /** prompt extend */
  private Boolean promptExtend;

  /** watermark */
  private Boolean watermark;

  /** picture size */
  private String size;

  /** number of images */
  private Integer n;

  private Boolean enableInterleave;

  private Boolean stream;

  private Integer maxImages;

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
    if (seed != null) {
      params.put(ApiKeywords.SEED, seed);
    }
    // Apply different logic based on model version
    if (ParamUtils.isQwenVersionThreeOrHigher(getModel())) {
      if (incrementalOutput != null) {
        params.put(ApiKeywords.INCREMENTAL_OUTPUT, incrementalOutput);
      }
    } else {
      if (Boolean.TRUE.equals(incrementalOutput)) {
        params.put(ApiKeywords.INCREMENTAL_OUTPUT, incrementalOutput);
      }
    }

    if (modalities != null) {
      params.put(ApiKeywords.MODALITIES, modalities);
    }

    if (responseFormat != null) {
      params.put("response_format", responseFormat);
    }

    if (negativePrompt != null) {
      params.put("negative_prompt", negativePrompt);
    }

    if (promptExtend != null) {
      params.put("prompt_extend", promptExtend);
    }

    if (watermark != null) {
      params.put("watermark", watermark);
    }

    if (size != null) {
      params.put("size", size);
    }

    if (n != null) {
      params.put("n", n);
    }

    if (enableInterleave != null) {
      params.put("enable_interleave", enableInterleave);
    }

    if (stream != null) {
      params.put("stream", stream);
    }

    if (maxImages != null) {
      params.put("max_images", maxImages);
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
