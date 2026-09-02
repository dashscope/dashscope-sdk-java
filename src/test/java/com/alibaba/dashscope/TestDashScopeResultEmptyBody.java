// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.PublicErrorDef;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.google.gson.JsonObject;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * A successful HTTP response carrying no payload must not be reported as an internal error. Gson
 * cannot parse an empty body, so {@code DashScopeResult} has to short-circuit it.
 */
public class TestDashScopeResultEmptyBody {

  private DashScopeResult flattenResult(Integer statusCode, String message) throws Exception {
    NetworkResponse response =
        NetworkResponse.builder()
            .headers(Collections.emptyMap())
            .message(message)
            .httpStatusCode(statusCode)
            .build();
    return new DashScopeResult().fromResponse(Protocol.HTTP, response, true);
  }

  private void assertEmptySuccess(DashScopeResult result, int expectedStatusCode) {
    assertEquals(expectedStatusCode, result.getStatusCode());
    assertTrue(result.getOutput() instanceof JsonObject);
    assertEquals(0, ((JsonObject) result.getOutput()).size());
    assertNotEquals(
        PublicErrorDef.INTERNAL_ERROR.getStatusCode(),
        result.getStatusCode(),
        "empty body was misreported as an internal error");
    assertNotEquals(PublicErrorDef.INTERNAL_ERROR.getErrorCode(), result.getCode());
  }

  @Test
  public void testNoContentIsEmptyResult() throws Exception {
    assertEmptySuccess(flattenResult(204, ""), 204);
  }

  @Test
  public void testAcceptedWithEmptyBodyIsEmptyResult() throws Exception {
    assertEmptySuccess(flattenResult(202, ""), 202);
  }

  @Test
  public void testOkWithEmptyBodyIsEmptyResult() throws Exception {
    assertEmptySuccess(flattenResult(200, ""), 200);
  }

  @Test
  public void testNullMessageIsEmptyResult() throws Exception {
    assertEmptySuccess(flattenResult(204, null), 204);
  }

  @Test
  public void testMissingStatusCodeStillYieldsEmptyResult() throws Exception {
    DashScopeResult result = flattenResult(null, "");
    assertTrue(result.getOutput() instanceof JsonObject);
    assertEquals(0, ((JsonObject) result.getOutput()).size());
  }

  @Test
  public void testJsonBodyIsParsedAndCarriesStatusCode() throws Exception {
    DashScopeResult result = flattenResult(200, "{\"id\":\"wh_123\",\"deleted\":true}");
    assertEquals(200, result.getStatusCode());
    JsonObject output = (JsonObject) result.getOutput();
    assertEquals("wh_123", output.get("id").getAsString());
    assertTrue(output.get("deleted").getAsBoolean());
  }

  @Test
  public void testMalformedBodyIsStillAnInternalError() throws Exception {
    DashScopeResult result = flattenResult(200, "not json at all");
    assertEquals(PublicErrorDef.INTERNAL_ERROR.getStatusCode(), result.getStatusCode());
    assertEquals(PublicErrorDef.INTERNAL_ERROR.getErrorCode(), result.getCode());
  }
}
