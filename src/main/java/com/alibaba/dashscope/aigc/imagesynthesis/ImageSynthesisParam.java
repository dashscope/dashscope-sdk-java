// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.aigc.imagesynthesis;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.GsonExclude;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.PreprocessInputImage;
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

  /** The specific functions to be achieved , see class ImageEditFunction */
  @Builder.Default private String function = null;

  /** Enter the URL address of the target edited image. */
  @Builder.Default private String baseImageUrl = null;

  /** Provide the URL address of the image of the marked area by the user.
   * It should be consistent with the image resolution of the base_image_url. */
  @Builder.Default private String maskImageUrl = null;

  /** The extra parameters. */
  @GsonExclude @Singular protected Map<String, Object> extraInputs;

  @Builder.Default private Boolean promptExtend = null;

  @Builder.Default private Boolean watermark = null;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(PROMPT, prompt);
    if (negativePrompt != null && !negativePrompt.isEmpty()) {
      jsonObject.addProperty(NEGATIVE_PROMPT, negativePrompt);
    }
    if (refImage != null && !refImage.isEmpty()) {
      jsonObject.addProperty(REF_IMG, refImage);
    }
    if (sketchImageUrl != null && !sketchImageUrl.isEmpty()) {
      jsonObject.addProperty(SKETCH_IMAGE_URL, sketchImageUrl);
    }

    if (function != null && !function.isEmpty()) {
      jsonObject.addProperty(FUNCTION, function);
    }

    if (baseImageUrl != null && !baseImageUrl.isEmpty()) {
      jsonObject.addProperty(BASE_IMAGE_URL, baseImageUrl);
    }

    if (maskImageUrl != null && !maskImageUrl.isEmpty()) {
      jsonObject.addProperty(MASK_IMAGE_URL, maskImageUrl);
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
    if (promptExtend != null) {
      params.put(PROMPT_EXTEND, promptExtend);
    }
    if (watermark != null) {
      params.put(WATERMARK, watermark);
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

  public void checkAndUpload() throws NoApiKeyException, UploadFileException {
    Map<String, String> inputChecks = new HashMap<>();
    inputChecks.put(REF_IMG, this.refImage);
    inputChecks.put(SKETCH_IMAGE_URL, this.sketchImageUrl);
    inputChecks.put(BASE_IMAGE_URL, this.baseImageUrl);
    inputChecks.put(MASK_IMAGE_URL, this.maskImageUrl);

    boolean isUpload = PreprocessInputImage.checkAndUploadImage(getModel(), inputChecks, getApiKey());

    if (isUpload) {
      this.putHeader("X-DashScope-OssResourceResolve", "enable");

      this.refImage = inputChecks.get(REF_IMG);
      this.sketchImageUrl = inputChecks.get(SKETCH_IMAGE_URL);
      this.baseImageUrl = inputChecks.get(BASE_IMAGE_URL);
      this.maskImageUrl = inputChecks.get(MASK_IMAGE_URL);
    }
  }
}
