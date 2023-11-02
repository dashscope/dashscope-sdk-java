// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.aigc.imagesynthesis;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class SketchImageSynthesisParam extends HalfDuplexServiceParam {
  @Builder.Default private Integer n = null;
  @Builder.Default private String size = null;

  @SerializedName("sketch_weight")
  @Builder.Default
  private Integer sketchWeight = null;

  @SerializedName("realisticness")
  @Builder.Default
  private Integer realisticness = null;

  @SerializedName("sketch_image_url")
  @lombok.NonNull
  private String sketchImageUrl;

  @lombok.NonNull private String prompt;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(PROMPT, prompt);
    jsonObject.addProperty(SKETCH_IMAGE_URL, sketchImageUrl);
    return jsonObject;
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (n != null) {
      params.put(NUMBER, n);
    }
    if (size != null) {
      params.put(SIZE, size);
    }
    if (sketchWeight != null) {
      params.put(SKETCH_WEIGHT, sketchWeight);
    }
    if (realisticness != null) {
      params.put(REALISTICNESS, realisticness);
    }
    params.putAll(super.getParameters());
    return params;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("model", getModel());
    body.add("input", getInput());
    if (getParameters() != null) {
      body.add("parameters", JsonUtils.parametersToJsonObject(getParameters()));
    }
    return body;
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {
    //
  }
}
