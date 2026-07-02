// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

/**
 * Tests that Result subclass {@code fromDashScopeResult} methods handle non-JsonObject output (e.g.
 * JsonPrimitive from encrypted or malformed responses) gracefully via the {@code instanceof
 * JsonObject} defensive check.
 */
public class TestResultTypeSafety {

  private DashScopeResult buildResultWithPrimitiveOutput() {
    DashScopeResult dsr = new DashScopeResult();
    dsr.setRequestId("req-ts-1");
    dsr.setOutput(new JsonPrimitive("not-a-json-object"));
    return dsr;
  }

  @Test
  public void testGenerationResultWithPrimitiveOutput() {
    DashScopeResult dsr = buildResultWithPrimitiveOutput();
    GenerationResult result = GenerationResult.fromDashScopeResult(dsr);
    assertNull(result.getOutput());
    assertEquals("req-ts-1", result.getRequestId());
  }

  @Test
  public void testConversationResultWithPrimitiveOutput() {
    DashScopeResult dsr = buildResultWithPrimitiveOutput();
    ConversationResult result = ConversationResult.fromDashScopeResult(dsr);
    assertNull(result.getOutput());
    assertEquals("req-ts-1", result.getRequestId());
  }

  @Test
  public void testGenerationResultWithJsonObjectOutput() {
    DashScopeResult dsr = new DashScopeResult();
    dsr.setRequestId("req-ts-2");
    JsonObject outputJson = JsonUtils.parse("{\"choices\":[],\"text\":\"hello\"}");
    dsr.setOutput(outputJson);

    GenerationResult result = GenerationResult.fromDashScopeResult(dsr);
    assertTrue(result.getOutput() != null);
    assertEquals("req-ts-2", result.getRequestId());
  }
}
