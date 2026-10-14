// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import lombok.EqualsAndHashCode;

/**
 * Session-level agent reference: an Agent ID string or an override object carrying
 * system/tools/mcp_servers/skills.
 */
@EqualsAndHashCode
public final class SessionAgent {
  private final String string;
  private final Map<String, Object> override;

  private SessionAgent(String string, Map<String, Object> override) {
    this.string = string;
    this.override = override;
  }

  /**
   * The Agent ID string form — pins the latest version for the session.
   *
   * @throws IllegalArgumentException if {@code agentId} is {@code null}.
   */
  public static SessionAgent ofString(String agentId) {
    if (agentId == null) {
      throw new IllegalArgumentException("agentId is required");
    }
    return new SessionAgent(agentId, null);
  }

  /**
   * The Agent override object form ({@code type=agent_with_overrides} with Session-level {@code
   * system}/{@code tools}/{@code mcp_servers}/{@code skills} overrides). The mapping is passed
   * through verbatim.
   *
   * @throws IllegalArgumentException if {@code override} is {@code null}.
   */
  public static SessionAgent ofOverride(Map<String, Object> override) {
    if (override == null) {
      throw new IllegalArgumentException("override is required");
    }
    return new SessionAgent(null, override);
  }

  /** @return {@code true} if this is the Agent ID string form. */
  public boolean isString() {
    return string != null;
  }

  /** @return the Agent ID string, or {@code null} if this is the override form. */
  public String asString() {
    return string;
  }

  /** @return the override mapping, or {@code null} if this is the string form. */
  public Map<String, Object> asOverride() {
    return override;
  }

  private static final Gson NULLS_GSON =
      new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

  /** Serialize this reference to JSON: a string primitive or an override object. */
  public JsonElement toJsonElement() {
    if (string != null) {
      return new JsonPrimitive(string);
    }
    return NULLS_GSON.toJsonTree(override);
  }

  /** Serialize a raw agent value (SessionAgent, String, or Map) to JSON. */
  public static JsonElement toJsonElement(Object agent) {
    if (agent == null) {
      throw new IllegalArgumentException("agent is required");
    }
    if (agent instanceof SessionAgent) {
      return ((SessionAgent) agent).toJsonElement();
    }
    if (agent instanceof String) {
      return new JsonPrimitive((String) agent);
    }
    if (agent instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> override = (Map<String, Object>) agent;
      return NULLS_GSON.toJsonTree(override);
    }
    throw new IllegalArgumentException(
        "agent must be a String (Agent ID), a Map (override), or a SessionAgent");
  }

  @Override
  public String toString() {
    return string != null
        ? "SessionAgent{string=" + string + "}"
        : "SessionAgent{override=" + override + "}";
  }
}
