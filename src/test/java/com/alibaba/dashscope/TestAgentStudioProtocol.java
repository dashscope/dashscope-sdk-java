// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.agentstudio.message.ClientEvents;
import com.alibaba.dashscope.agentstudio.message.ContentBlock;
import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.agentstudio.model.Configs;
import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.agentstudio.param.AgentCreateParam;
import com.alibaba.dashscope.agentstudio.param.SessionEventListParam;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Focused unit tests for the AgentStudio protocol surface (tool approval, stop_reason, event
 * deltas, types[] query, message accessors). No network — pure model/helper assertions.
 */
class TestAgentStudioProtocol {

  private static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(ContentBlock.class, new ContentBlock.Deserializer())
          .create();

  @Test
  void testEventListTypesRepeatedQuery() {
    // types must be repeated types[]= keys, not a single comma-joined value.
    String query =
        SessionEventListParam.builder()
            .types(Arrays.asList("message", "tool_call", "tool_approval_request"))
            .limit(50)
            .build()
            .toQueryString();
    assertTrue(query.contains("types[]=message"), query);
    assertTrue(query.contains("types[]=tool_call"), query);
    assertTrue(query.contains("types[]=tool_approval_request"), query);
    assertFalse(query.contains(","), "types should not be comma-joined: " + query);
  }

  @Test
  void testUserToolApprovalResponseWire() {
    JsonObject allow = ClientEvents.userToolApprovalResponse("b1", "c1", "allow", null);
    assertEquals("tool_approval_response", allow.get("type").getAsString());
    assertEquals("user", allow.get("role").getAsString());
    JsonObject data =
        allow.getAsJsonArray("content").get(0).getAsJsonObject().getAsJsonObject("data");
    assertEquals("b1", data.get("batch_id").getAsString());
    assertEquals("c1", data.get("call_id").getAsString());
    assertEquals("allow", data.get("result").getAsString());
    assertFalse(data.has("deny_message"));

    JsonObject deny = ClientEvents.userToolApprovalResponse("b1", "c2", "deny", "nope");
    JsonObject denyData =
        deny.getAsJsonArray("content").get(0).getAsJsonObject().getAsJsonObject("data");
    assertEquals("deny", denyData.get("result").getAsString());
    assertEquals("nope", denyData.get("deny_message").getAsString());
  }

  @Test
  void testUserToolApprovalResponseInvalidResult() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ClientEvents.userToolApprovalResponse("b1", "c1", "maybe", null));
  }

  @Test
  void testStopReasonPendingFields() {
    Session.StopReason sr = new Session.StopReason();
    sr.setType("requires_action");
    sr.setPendingBatchId("response_xxx:9f2c");
    sr.setPendingCallIds(Arrays.asList("call_1", "call_2"));
    assertEquals("requires_action", sr.getType());
    assertEquals("response_xxx:9f2c", sr.getPendingBatchId());
    assertEquals(Arrays.asList("call_1", "call_2"), sr.getPendingCallIds());
  }

  @Test
  void testMessageDeltaText() {
    String json =
        "{\"type\":\"event_delta\",\"event_id\":\"sevt_a\","
            + "\"delta\":{\"type\":\"content_delta\",\"index\":0,"
            + "\"content\":{\"type\":\"text\",\"text\":\"hello\"}}}";
    Message msg = GSON.fromJson(json, Message.class);
    assertEquals("event_delta", msg.getType());
    assertEquals("sevt_a", msg.getDeltaEventId());
    assertEquals("hello", msg.getDeltaText());
  }

  @Test
  void testMessageEventStart() {
    String json = "{\"type\":\"event_start\",\"event\":{\"id\":\"sevt_a\",\"type\":\"message\"}}";
    Message msg = GSON.fromJson(json, Message.class);
    assertEquals("event_start", msg.getType());
    JsonObject event = msg.getEventStart();
    assertEquals("sevt_a", event.get("id").getAsString());
    assertEquals("message", event.get("type").getAsString());
  }

  @Test
  void testMessageToolApprovalRequestAccessor() {
    String json =
        "{\"object\":\"message\",\"status\":\"completed\",\"id\":\"m1\","
            + "\"role\":\"assistant\",\"type\":\"tool_approval_request\","
            + "\"content\":[{\"type\":\"data\",\"data\":{\"batch_id\":\"b1\","
            + "\"call_id\":\"c1\",\"name\":\"bash\",\"tool_type\":\"builtin\"}}]}";
    Message msg = GSON.fromJson(json, Message.class);
    JsonObject ap = msg.getToolApprovalRequest();
    assertEquals("b1", ap.get("batch_id").getAsString());
    assertEquals("c1", ap.get("call_id").getAsString());
    assertEquals("bash", ap.get("name").getAsString());
  }

  @Test
  void testMessageErrorAccessor() {
    String json =
        "{\"type\":\"error\",\"error\":{\"code\":\"tool_approval_service_unavailable\","
            + "\"message\":\"down\"},\"metadata\":{\"pending_tool_approvals\":"
            + "{\"batch_id\":\"b1\",\"call_ids\":[\"c1\"]}}}";
    Message msg = GSON.fromJson(json, Message.class);
    assertEquals("error", msg.getType());
    assertEquals("tool_approval_service_unavailable", msg.getError().get("code").getAsString());
    assertEquals("down", msg.getError().get("message").getAsString());
    assertEquals("b1", msg.getPendingToolApprovals().get("batch_id"));
  }

  @Test
  void testMessageAccessorsReturnNullForNonMatchingType() {
    Message msg = new Message();
    msg.setType("message");
    assertNull(msg.getToolApprovalRequest());
    assertNull(msg.getError());
    assertNull(msg.getPendingToolApprovals());
    assertNull(msg.getEventStart());
    assertNull(msg.getDeltaText());
  }

  @Test
  void testPermissionPolicySerializedInToolConfig() {
    Configs.ToolConfig.DefaultConfig dc = new Configs.ToolConfig.DefaultConfig();
    dc.setEnabled(false);
    Configs.PermissionPolicy pp = new Configs.PermissionPolicy();
    pp.setType("always_ask");
    dc.setPermissionPolicy(pp);

    Configs.ToolConfig tool = new Configs.ToolConfig();
    tool.setType("builtin_toolkit");
    tool.setDefaultConfig(dc);

    JsonObject body =
        AgentCreateParam.builder()
            .name("test")
            .model("qwen-max")
            .tools(Arrays.asList(tool))
            .build()
            .getHttpBody();

    JsonObject dcJson =
        body.getAsJsonArray("tools").get(0).getAsJsonObject().getAsJsonObject("default_config");
    assertTrue(dcJson.has("permission_policy"));
    assertEquals(
        "always_ask", dcJson.getAsJsonObject("permission_policy").get("type").getAsString());
  }
}
