package com.alibaba.dashscope.threads;

import com.alibaba.dashscope.common.TypeRegistry;
import lombok.Data;

/** tool request base */
@Data
public abstract class ContentBase {
  private static final TypeRegistry<ContentBase> contentRegistry = new TypeRegistry<>();

  protected static synchronized void registerContent(
      String type, Class<? extends ContentBase> clazz) {
    contentRegistry.register(type, clazz);
  }

  public static synchronized Class<? extends ContentBase> getContentClass(String type) {
    return contentRegistry.get(type);
  }

  // register official tools for list.
  static {
    registerContent("image_file", ContentImageFile.class);
    registerContent("text", ContentText.class);
  }

  public abstract String getType();
}
