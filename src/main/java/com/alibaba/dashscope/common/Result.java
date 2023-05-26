package com.alibaba.dashscope.common;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.Response;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Result {

  /** The request id. */
  private String requestId;

  /** The usage information */
  private JsonObject usage;

  /** The headers of response */
  private Map<String, String> headers;

  /**
   * Load data from the server output.
   *
   * @param protocol The protocol, please check the `Protocol` enum.
   * @param response The response.
   * @throws ApiException if the input is invalid.
   */
  public abstract void fromResponse(Protocol protocol, Response response) throws ApiException;
}
