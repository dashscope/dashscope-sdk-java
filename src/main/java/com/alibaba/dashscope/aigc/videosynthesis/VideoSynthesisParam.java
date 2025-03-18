// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.videosynthesis;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.Constants;
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
public class VideoSynthesisParam extends HalfDuplexServiceParam {

  @Builder.Default private String size = VideoSynthesis.Size.DEFAULT;

  @Builder.Default private Integer steps = null;

  @Builder.Default private Integer seed = null;

  @Builder.Default private String prompt = null;

  /** The negative prompt is the opposite of the prompt meaning. */
  @Builder.Default private String negative_prompt = null;

  /** LoRa input, such as gufeng, katong, etc. */
  @Builder.Default private String template = null;

  /** Whether to enable write expansion. The default value is True. */
  @Builder.Default private Boolean extendPrompt = Boolean.TRUE;

  /** The input image url, Generate the URL of the image referenced by the video */
  @Builder.Default private String imgUrl = null;

  /** The extra parameters. */
  @GsonExclude @Singular protected Map<String, Object> extraInputs;

  /** Duration of video generation. The default value is 5, in seconds */
  @Builder.Default private Integer duration = VideoSynthesis.Duration.DEFAULT;

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
    if (template != null && !template.isEmpty()) {
      jsonObject.addProperty(TEMPLATE, template);
    }
    if (imgUrl != null && !imgUrl.isEmpty()) {
      jsonObject.addProperty(IMG_URL, imgUrl);
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
    params.put(DURATION, duration);

    params.put(SIZE, size);

    if (steps != null) {
      params.put(STEPS, steps);
    }
    if (seed != null) {
      params.put(SEED, seed);
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
    if (getParameters() != null) {
      body.add(PARAMETERS, JsonUtils.parametersToJsonObject(getParameters()));
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
}
