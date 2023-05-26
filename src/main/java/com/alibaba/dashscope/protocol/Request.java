package com.alibaba.dashscope.protocol;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Request {

  /** The JsonObject type message to send. */
  private String message;

  /** The Binary type message to send. */
  private ByteBuffer binary;
}
