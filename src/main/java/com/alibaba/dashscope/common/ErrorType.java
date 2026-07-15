// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public enum ErrorType {

  /** Network error: DNS failure, connection refused, etc. API did not respond. */
  NETWORK_ERROR("network error"),

  /** WebSocket connection failed after retries. API returned no content. */
  CONNECTION_ERROR("ConnectionError"),

  /** Asynchronous task polling timed out. API is still processing. */
  TASK_WAIT_TIMEOUT("TaskWaitTimeout"),

  /** HTTP TTS waiting for audio data timed out. API returned no error. */
  REQUEST_TIMEOUT("RequestTimeOut"),

  /** JSON parsing failed. Original body preserved in message field. */
  JSON_PARSE_ERROR("json_parse_error"),

  /** Failed to read response body due to IOException. HTTP status code preserved. */
  BODY_READ_ERROR("body_read_error"),

  /** Non-JSON content type received (e.g., HTML error page). Original body in message. */
  NON_JSON_RESPONSE("non_json_response"),
  ;

  private final String value;

  private ErrorType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
