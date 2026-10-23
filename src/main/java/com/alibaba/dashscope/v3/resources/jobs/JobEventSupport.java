// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.jobs;

import com.alibaba.dashscope.v3.errors.ApiStatusException;
import com.alibaba.dashscope.v3.errors.Errors;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.sse.JobEventStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Shared payload/error mapping for the job-event streams (fine-tunes/deployments; Python v2 _sse.py
 * parity): payloads parse as JSON when possible, and an {@code event:error} frame maps through the
 * catalog — {@code status:} absent means a business failure on an HTTP-200 stream, so the body's
 * own code picks the class.
 */
public final class JobEventSupport {
  private JobEventSupport() {}

  public static JsonNode tryJson(String data) {
    if (data == null) {
      return null;
    }
    try {
      return JsonSupport.readTree(data);
    } catch (RuntimeException e) {
      return TextNode.valueOf(data);
    }
  }

  /** One {@code event:error} frame → the mapped exception (Python v2 _stream_error parity). */
  public static ApiStatusException toException(JobEventStream.Frame frame) {
    String data = frame.getData();
    JsonNode parsed = tryJson(data);
    boolean isObject = parsed != null && parsed.isObject();
    String code = isObject ? JsonSupport.errorCode(parsed) : null;
    String message =
        isObject && JsonSupport.errorMessage(parsed) != null
            ? JsonSupport.errorMessage(parsed)
            : data;
    Object body = isObject ? JsonSupport.toPlainValue(parsed) : data;
    if (frame.getStatusCode() == null) {
      return Errors.fromBusinessCode(code, message, null, body, null);
    }
    return Errors.fromStatus(frame.getStatusCode(), code, message, null, body, null);
  }
}
