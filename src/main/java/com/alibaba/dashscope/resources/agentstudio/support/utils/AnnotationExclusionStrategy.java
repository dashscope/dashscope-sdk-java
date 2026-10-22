// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/** 3.0 copy of the 2.x strategy: skip fields annotated with {@link GsonExclude}. */
public class AnnotationExclusionStrategy implements ExclusionStrategy {

  @Override
  public boolean shouldSkipField(FieldAttributes f) {
    return f.getAnnotation(GsonExclude.class) != null;
  }

  @Override
  public boolean shouldSkipClass(Class<?> clazz) {
    return false;
  }
}
