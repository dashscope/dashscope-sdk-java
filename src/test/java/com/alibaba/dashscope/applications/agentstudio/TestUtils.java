// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio;

import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;

/** AgentStudio-owned mock helpers, independent of the legacy test utilities. */
public class TestUtils {
  private static final MediaType APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");

  public static MockResponse createMockResponse(String responseBody, int statusCode) {
    return new MockResponse()
        .setHeader("Content-Type", APPLICATION_JSON)
        .setResponseCode(statusCode)
        .setBody(responseBody);
  }
}
