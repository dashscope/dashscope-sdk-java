// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.generation.models;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.aigc.generation.GenerationParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class BailianParam extends GenerationParamBase {
  /* The maximum length of tokens to generate.
  The token count of your prompt plus max_length
  cannot exceed the model's context length. Most models
  have a context length of 2000 tokens */
  @Builder.Default private Integer maxLength = null;
  /* A sampling strategy, called nucleus
  sampling, where the model considers the results of the
  tokens with top_p probability mass. So 0.1 means only
  the tokens comprising the top 10% probability mass are
  considered */
  @Builder.Default private Double topP = null;
  /* The enterprise-specific
  large model id, which needs to be generated from the
  operation background of the enterprise-specific
  large model product */
  @lombok.NonNull private String customizedModelId;

  protected JsonObject getInputs() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(CUSTOMIZED_MODEL_ID, customizedModelId);
    return JsonUtils.merge(jsonObject, super.getHttpBody());
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (maxLength != null) {
      params.put("max_length", params);
    }
    if (topP != null) {
      params.put("top_p", topP);
    }
    params.putAll(parameters);
    return params;
  }
}
