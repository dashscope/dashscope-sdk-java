// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.base;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** The model service base class */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class FullDuplexServiceParam extends FullDuplexParamBase {
  @Default private Object resources = null;
  /**
   * Get the resources
   *
   * @return the resource object.
   */
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
    if (headers.size() == 0) {
      headers = new HashMap<String, Object>();
    } else {
      HashMap<String, Object> newHeaders = new HashMap<>();
      newHeaders.putAll(headers);
      headers = newHeaders;
      headers.put(key, value);
    }
  }
}
