// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.base;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** The model service base class */
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public abstract class HalfDuplexServiceParam extends HalfDuplexParamBase {
  /** The model to use. */
  @lombok.NonNull private String model;

  @Default private Object resources = null;

  @lombok.NonNull
  @Override
  public String getModel() {
    return model;
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @Override
  public Object getResources() {
    return resources;
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> res = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().toString();
      res.put(key, value);
    }
    return res;
  }

  public void putHeader(String key, String value) {
    if (headers.isEmpty()) {
      headers = new HashMap<>();
    } else {
      headers = new HashMap<>(headers);
    }
    headers.put(key, value);
  }
}
