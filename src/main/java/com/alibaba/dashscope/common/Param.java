package com.alibaba.dashscope.common;

import com.alibaba.dashscope.protocol.Request;
import com.alibaba.dashscope.utils.Constants;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Data
@SuperBuilder
public abstract class Param {

  /** The request id */
  private String requestId;

  /** The model to use. */
  private String model;

  /** The apiKey. */
  private String apiKey;

  /** Get real-time response on streaming mode */
  @Builder.Default private StreamingMode mode = StreamingMode.NONE;

  public String getApiKey() {
    return apiKey == null ? Constants.apiKey : apiKey;
  }

  /** Open security check */
  @Builder.Default private boolean securityCheck = false;

  public String getRequestId() {
    if (StringUtils.isBlank(requestId)) {
      requestId = UUID.randomUUID().toString();
    }
    return requestId;
  }

  public abstract String url();

  public abstract Request toRequest(Protocol protocol);

  /**
   * Get the related output class, which should be a subclass of `Result`.
   *
   * @return The output Class.
   */
  public abstract Class<? extends Result> resultType();
}
