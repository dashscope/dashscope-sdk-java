// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.aigc.imagesynthesis;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.GsonExclude;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ImageSynthesisParam extends HalfDuplexServiceParam {
  @Builder.Default private Integer n = null;
  @Builder.Default private String size = null;
  @Builder.Default private Integer steps = null;
  @Builder.Default private Integer scale = null;
  @Builder.Default private Integer seed = null;
  @Builder.Default private String style = null;
  @lombok.NonNull private String prompt;
  private String negativePrompt;
  private String refImage;
  private String sketchImageUrl;

  /** The extra parameters. */
  @GsonExclude @Singular protected Map<String, Object> extraInputs;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(PROMPT, prompt);
    if (negativePrompt != null && !negativePrompt.isEmpty()) {
      jsonObject.addProperty(NEGATIVE_PROMPT, negativePrompt);
    }
    if (refImage != null && !refImage.isEmpty()) {
      jsonObject.addProperty("ref_img", refImage);
    }
    if (sketchImageUrl != null && !sketchImageUrl.isEmpty()) {
      jsonObject.addProperty("sketch_image_url", sketchImageUrl);
    }
    if (extraInputs != null && !extraInputs.isEmpty()) {
      JsonObject extraInputsJsonObject = JsonUtils.parametersToJsonObject(extraInputs);
      jsonObject = JsonUtils.merge(jsonObject, extraInputsJsonObject);
    }
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
    if (negativePrompt != null) {
      params.put(NEGATIVE_PROMPT, negativePrompt);
    }
    if (steps != null) {
      params.put(STEPS, steps);
    }
    if (scale != null) {
      params.put(SCALE, scale);
    }
    if (seed != null) {
      params.put(SEED, seed);
    }
    if (style != null) {
      params.put(STYLE, style);
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
