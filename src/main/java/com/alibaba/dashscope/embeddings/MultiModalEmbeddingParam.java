// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class MultiModalEmbeddingParam extends HalfDuplexServiceParam {
  @NonNull private List<MultiModalEmbeddingItemBase> contents;

  /**
   * The output vector dimensions. Supported values vary by model. For example,
   * multimodal-embedding-v1 outputs 1024-dimension vectors only and ignores this parameter, while
   * tongyi-embedding-vision-plus supports multiple dimensions (1152 by default).
   */
  private Integer dimension;

  /** The output vector format. Currently only "dense" is supported. */
  private String outputType;

  /** The video frame extraction ratio, in range [0, 1]. Default: 1.0. */
  private Double fps;

  /** Custom task instruction to guide model understanding of query intent. */
  private String instruct;

  /**
   * Only applicable to qwen3-vl-embedding. When true, all contents are fused into a single vector.
   */
  private Boolean enableFusion;

  /** The resolution level of the input images/videos. */
  private Integer resLevel;

  /** The maximum number of frames extracted from the input video. */
  private Integer maxVideoFrames;

  public List<MultiModalEmbeddingItemBase> getContent() {
    return contents;
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (dimension != null) {
      params.put("dimension", dimension);
    }
    if (outputType != null) {
      params.put("output_type", outputType);
    }
    if (fps != null) {
      params.put("fps", fps);
    }
    if (instruct != null) {
      params.put("instruct", instruct);
    }
    if (enableFusion != null) {
      params.put("enable_fusion", enableFusion);
    }
    if (resLevel != null) {
      params.put("res_level", resLevel);
    }
    if (maxVideoFrames != null) {
      params.put("max_video_frames", maxVideoFrames);
    }
    if (parameters != null && !parameters.isEmpty()) {
      params.putAll(parameters);
    }
    return params;
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
  public JsonObject getInput() {
    JsonObject input = new JsonObject();
    input.add("contents", JsonUtils.toJsonArray(contents));
    return input;
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {
    if (contents.isEmpty()) {
      throw new InputRequiredException("contents must not empty");
    }
  }
}
