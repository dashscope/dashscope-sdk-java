// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.base;

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
}
