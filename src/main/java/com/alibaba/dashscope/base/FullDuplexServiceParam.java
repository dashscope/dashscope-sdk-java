// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.base;

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
   * @return
   */
  public Object getResources() {
    return resources;
  }
}
