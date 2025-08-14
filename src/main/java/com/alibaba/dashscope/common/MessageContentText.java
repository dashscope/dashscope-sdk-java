package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MessageContentText extends MessageContentBase {
  @Data
  @SuperBuilder
  public static class CacheControl {
     private String type;
     private String ttl;
  }

  @Builder.Default private String type = "text";

  private String text;

  @SerializedName("cache_control")
  private CacheControl cacheControl;
}
