// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/** Excludes fields annotated with {@link GsonExclude} from Gson serialization. */
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
