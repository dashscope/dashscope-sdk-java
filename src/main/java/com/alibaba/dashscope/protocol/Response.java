package com.alibaba.dashscope.protocol;

import com.google.common.collect.Maps;
import java.nio.ByteBuffer;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Response {

  /** The response headers. */
  @Builder.Default private Map<String, String> headers = Maps.newHashMap();

  /** The String type response message. */
  private String message;

  /** The binary type response. */
  private ByteBuffer binary;
}
