// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.applications.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.applications.agentstudio.exception.InputRequiredException;
import com.alibaba.dashscope.applications.agentstudio.param.UsageQueryParam;
import com.alibaba.dashscope.applications.agentstudio.resource.Usage;
import com.alibaba.dashscope.applications.agentstudio.utils.JsonUtils;
import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.SdkException;
import com.google.gson.JsonObject;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

public class TestAgentStudioUsage {
  @Test
  public void sessionSummaryPreservesNullAndUnits() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(
          json(
              "{\"request_id\":\"req-summary\",\"data\":{\"usage\":{\"total\":{\"total_tokens\":{\"value\":null,\"unit\":\"tokens\"}},\"models\":[],\"tools\":[]}}}"));
      try (AgentStudioClient client = client(server)) {
        Usage.Result result =
            client
                .usage()
                .summary(
                    UsageQueryParam.builder().dimension("session").sessionId("sesn-test").build());
        assertEquals("req-summary", result.getRequestId());
        assertTrue(
            result
                .getData()
                .getAsJsonObject("usage")
                .getAsJsonObject("total")
                .getAsJsonObject("total_tokens")
                .get("value")
                .isJsonNull());
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("POST", request.getMethod());
        assertEquals("/agentstudio/usage/summary", request.getPath());
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
        JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
        assertEquals(2, body.size());
        assertEquals("sesn-test", body.get("session_id").getAsString());
      }
    }
  }

  @Test
  public void asyncTrendKeepsBodyAndCursor() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(
          json(
              "{\"request_id\":\"req-trend\",\"data\":{\"points\":[],\"has_more\":true,\"next_page_token\":\"opaque+/=\"}}"));
      server.enqueue(
          json(
              "{\"request_id\":\"req-last\",\"data\":{\"points\":[],\"has_more\":false,\"next_page_token\":null}}"));
      try (AgentStudioClient client = client(server)) {
        UsageQueryParam param =
            UsageQueryParam.builder()
                .dimension("agent")
                .agentId("agent-test")
                .agentVersion("1")
                .granularity("hour")
                .pageSize(1)
                .timeRange(
                    UsageQueryParam.TimeRange.builder()
                        .startTime("2026-11-02T10:15:30+08:00")
                        .endTime("2026-11-02T14:00:01+08:00")
                        .build())
                .build();
        Usage.Result first = client.usage().trendAsync(param).get(5, TimeUnit.SECONDS);
        param.setPageToken(first.getData().get("next_page_token").getAsString());
        Usage.Result last = client.usage().trend(param);
        assertFalse(last.getData().get("has_more").getAsBoolean());
        assertTrue(last.getData().get("next_page_token").isJsonNull());
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("/agentstudio/usage/trend", request.getPath());
        JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
        assertFalse(body.has("page_token"));
        assertEquals(
            "2026-11-02T10:15:30+08:00",
            body.getAsJsonObject("time_range").get("start_time").getAsString());
        JsonObject next =
            JsonUtils.parse(server.takeRequest(5, TimeUnit.SECONDS).getBody().readUtf8());
        assertEquals("opaque+/=", next.remove("page_token").getAsString());
        assertEquals(body, next);
      }
    }
  }

  @Test
  public void summaryAsyncAndServiceErrors() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(
          json(
              "{\"request_id\":\"req-large\",\"data\":{\"value\":9007199254740993,\"ratio\":0.4}}"));
      server.enqueue(
          json("{\"type\":\"error\",\"error\":{\"code\":\"page_token_expired\",\"message\":\"expired\"},\"request_id\":\"req-error\"}")
              .setResponseCode(410));
      try (AgentStudioClient client = client(server)) {
        UsageQueryParam param = UsageQueryParam.builder().dimension("workspace").build();
        Usage.Result result = client.usage().summaryAsync(param).get(5, TimeUnit.SECONDS);
        assertEquals(9007199254740993L, result.getData().get("value").getAsLong());
        assertEquals(0.4, result.getData().get("ratio").getAsDouble());
        assertThrows(ApiStatusException.class, () -> client.usage().trend(param));
        SdkException missing = assertThrows(SdkException.class, () -> client.usage().summary(null));
        assertTrue(missing.getCause() instanceof InputRequiredException);
      }
    }
  }

  private AgentStudioClient client(MockWebServer server) {
    return AgentStudioClient.builder()
        .baseUrl(server.url("/agentstudio").toString())
        .apiKey("test-key")
        .build();
  }

  private MockResponse json(String body) {
    return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
  }
}
