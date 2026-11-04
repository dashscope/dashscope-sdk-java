package com.alibaba.dashscope.legacy_2x.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

@Deprecated
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
