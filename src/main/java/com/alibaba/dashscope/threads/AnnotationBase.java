package com.alibaba.dashscope.threads;

import com.alibaba.dashscope.common.TypeRegistry;
import com.google.gson.annotations.SerializedName;

public class AnnotationBase {
  /**
   * End Index
   *
   * <p>(Required)
   */
  @SerializedName("end_index")
  private Integer endIndex;
  /**
   * Start Index
   *
   * <p>(Required)
   */
  @SerializedName("start_index")
  private Integer startIndex;
  /**
   * Text
   *
   * <p>(Required)
   */
  @SerializedName("text")
  private String text;
  /**
   * Type
   *
   * <p>(Required)
   */
  @SerializedName("type")
  private Object type;

  private static final TypeRegistry<AnnotationBase> annotationRegistry = new TypeRegistry<>();

  protected static synchronized void registerAnnotation(
      String type, Class<? extends AnnotationBase> clazz) {
    annotationRegistry.register(type, clazz);
  }

  public static synchronized Class<? extends AnnotationBase> getAnnotationClass(String type) {
    return annotationRegistry.get(type);
  }
  // register official tools for list.
  static {
    registerAnnotation("file_citation", FileCitationAnnotation.class);
    registerAnnotation("file_path", FilePathAnnotation.class);
  }
}
