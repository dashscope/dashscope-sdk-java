// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.videosynthesis;

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
public class VideoSynthesisParam extends HalfDuplexServiceParam {

  @Builder.Default private Map<String, String> inputChecks = new HashMap<>();

  @Builder.Default private String size = null;

  @Builder.Default private Integer steps = null;

  @Builder.Default private Integer seed = null;

  @Builder.Default private String prompt = null;

  /** The negative prompt is the opposite of the prompt meaning. use negativePrompt */
  @Deprecated
  @Builder.Default private String negative_prompt = null;

  /** The negative prompt is the opposite of the prompt meaning. */
  @Builder.Default private String negativePrompt = null;

  /** LoRa input, such as gufeng, katong, etc. */
  @Builder.Default private String template = null;

  /** use promptExtend in parameters */
  @Deprecated
  @Builder.Default private Boolean extendPrompt = Boolean.TRUE;

  /** The input image url, Generate the URL of the image referenced by the video */
  @Builder.Default private String imgUrl = null;

  /** The extra parameters. */
  @GsonExclude @Singular protected Map<String, Object> extraInputs;

  /** Duration of video generation. The default value is 5, in seconds */
  @Builder.Default private Integer duration = null;

  /** The URL of the first frame image for generating the video. */
  @Builder.Default private String firstFrameUrl = null;

  /** The URL of the last frame image for generating the video. */
  @Builder.Default private String lastFrameUrl = null;

  @Builder.Default private String headFrame = null;

  @Builder.Default private String tailFrame = null;

  @Builder.Default private Boolean withAudio = Boolean.FALSE;

  /** The resolution of the generated video */
  @Builder.Default private String resolution = null;

  @Builder.Default private Boolean promptExtend = null;

  @Builder.Default private Boolean watermark = null;

  /** The inputs of the model. */
  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    if (prompt != null && !prompt.isEmpty()) {
      jsonObject.addProperty(PROMPT, prompt);
    }

    jsonObject.addProperty(EXTEND_PROMPT, extendPrompt);

    if (negative_prompt != null && !negative_prompt.isEmpty()) {
      jsonObject.addProperty(NEGATIVE_PROMPT, negative_prompt);
    }
    if (negativePrompt != null && !negativePrompt.isEmpty()) {
      jsonObject.addProperty(NEGATIVE_PROMPT, negativePrompt);
    }
    if (template != null && !template.isEmpty()) {
      jsonObject.addProperty(TEMPLATE, template);
    }
    if (imgUrl != null && !imgUrl.isEmpty()) {
      jsonObject.addProperty(IMG_URL, imgUrl);
    }

    if (firstFrameUrl != null && !firstFrameUrl.isEmpty()) {
      jsonObject.addProperty(FIRST_FRAME_URL, firstFrameUrl);
    }

    if (lastFrameUrl != null && !lastFrameUrl.isEmpty()) {
      jsonObject.addProperty(LAST_FRAME_URL, lastFrameUrl);
    }

    if (headFrame != null && !headFrame.isEmpty()) {
      jsonObject.addProperty(HEAD_FRAME, headFrame);
    }

    if (tailFrame != null && !tailFrame.isEmpty()) {
      jsonObject.addProperty(TAIL_FRAME, tailFrame);
    }

    if (extraInputs != null && !extraInputs.isEmpty()) {
      JsonObject extraInputsJsonObject = JsonUtils.parametersToJsonObject(extraInputs);
      JsonUtils.merge(jsonObject, extraInputsJsonObject);
    }
    return jsonObject;
  }

  /** The parameters of the model. */
  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (duration != null) {
      params.put(DURATION, duration);
    }

    if (size != null) {
      params.put(SIZE, size);
    }

    if (resolution != null) {
      params.put(RESOLUTION, resolution);
    }

    params.put(WITH_AUDIO, withAudio);

    if (steps != null) {
      params.put(STEPS, steps);
    }
    if (seed != null) {
      params.put(SEED, seed);
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

  /** The http body of the request. */
  @Override
  public JsonObject getHttpBody() {
    // this.validate();
    JsonObject body = new JsonObject();
    body.addProperty(MODEL, getModel());
    body.add(INPUT, getInput());
    Map<String, Object> params = getParameters();
    if (params != null) {
      body.add(PARAMETERS, JsonUtils.parametersToJsonObject(params));
    }
    return body;
  }

  /** The binary data of the request. */
  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  /** Validate all parameters. */
  @Override
  public void validate() throws InputRequiredException {}

  public void checkAndUpload() throws NoApiKeyException, UploadFileException {
    Map<String, String> inputChecks = new HashMap<>();
    inputChecks.put(IMG_URL, this.imgUrl);
    inputChecks.put(FIRST_FRAME_URL, this.firstFrameUrl);
    inputChecks.put(LAST_FRAME_URL, this.lastFrameUrl);
    inputChecks.put(HEAD_FRAME, this.headFrame);
    inputChecks.put(TAIL_FRAME, this.tailFrame);

    boolean isUpload = PreprocessInputImage.checkAndUploadImage(getModel(), inputChecks, getApiKey());

    if (isUpload) {
      this.putHeader("X-DashScope-OssResourceResolve", "enable");

      this.imgUrl = inputChecks.get(IMG_URL);
      this.firstFrameUrl = inputChecks.get(FIRST_FRAME_URL);
      this.lastFrameUrl = inputChecks.get(LAST_FRAME_URL);
      this.headFrame = inputChecks.get(HEAD_FRAME);
      this.tailFrame = inputChecks.get(TAIL_FRAME);
    }
  }

}
