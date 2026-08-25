// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class AgentStudioConstants {
  public static final String BASE_URL_TEMPLATE =
      "https://%s.%s.maas.aliyuncs.com/api/v1/agentstudio";
  public static final String DEFAULT_REGION = "cn-beijing";
  public static final int DEFAULT_TIMEOUT_MS = 600_000;
  public static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
  public static final String ENV_BASE_URL = "DASHSCOPE_AGENTSTUDIO_URL";
  public static final String ENV_BASE_URL_ALT = "AGENTSTUDIO_URL";
  public static final String ENV_WORKSPACE = "DASHSCOPE_WORKSPACE";
  public static final String ENV_REGION = "DASHSCOPE_API_REGION";

  private AgentStudioConstants() {}

  public static final class SSEEventType {
    public static final String MESSAGE = "message";
    public static final String INTERRUPT = "interrupt";
    public static final String TOOL_CONFIRMATION = "tool_confirmation";
    public static final String FUNCTION_CALL_OUTPUT = "function_call_output";
    public static final String TOOL_CALL_OUTPUT = "tool_call_output";
    public static final String DEFINE_OUTCOME = "define_outcome";
    public static final String FUNCTION_CALL = "function_call";
    public static final String TOOL_CALL = "tool_call";
    public static final String REASONING = "reasoning";
    public static final String MCP_CALL = "mcp_call";
    public static final String MCP_CALL_OUTPUT = "mcp_call_output";
    public static final String SESSION_STATUS = "session_status";
    public static final String ERROR = "error";
    public static final String SESSION_UPDATED = "session_updated";
    public static final String THREAD_CREATED = "thread_created";
    public static final String THREAD_STATUS = "thread_status";
    public static final String THREAD_MESSAGE_SENT = "thread_message_sent";
    public static final String THREAD_MESSAGE_RECEIVED = "thread_message_received";
    public static final String THREAD_CONTEXT_COMPACTED = "thread_context_compacted";
    public static final String MODEL_REQUEST_START = "model_request_start";
    public static final String MODEL_REQUEST_END = "model_request_end";
    public static final String OUTCOME_EVALUATION = "outcome_evaluation";

    private SSEEventType() {}
  }

  public static final class MessageRole {
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String TOOL = "tool";

    private MessageRole() {}
  }

  public static final class BlockType {
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String AUDIO = "audio";
    public static final String DATA = "data";
    public static final String FILE = "file";
    public static final String REFUSAL = "refusal";
    public static final String ERROR = "error";

    private BlockType() {}
  }

  public static final class SessionStatusValue {
    public static final String IDLE = "idle";
    public static final String RUNNING = "running";
    public static final String RESCHEDULING = "rescheduling";
    public static final String TERMINATED = "terminated";

    private SessionStatusValue() {}
  }

  public static String resolveBaseUrl(String workspace, String region) {
    String ws = workspace;
    if (ws == null || ws.isEmpty()) {
      ws = System.getenv(ENV_WORKSPACE);
    }
    if (ws == null || ws.isEmpty()) {
      throw new IllegalArgumentException(
          "workspace is required. Pass it to the constructor, "
              + "set DASHSCOPE_WORKSPACE env var, or set DASHSCOPE_AGENTSTUDIO_URL.");
    }
    String r = region;
    if (r == null || r.isEmpty()) {
      r = System.getenv(ENV_REGION);
    }
    if (r == null || r.isEmpty()) {
      r = DEFAULT_REGION;
    }
    return String.format(BASE_URL_TEMPLATE, ws, r);
  }

  /**
   * Build a {@link GeneralServiceOption} for an agentstudio endpoint. When {@code baseUrl} is
   * non-null it overrides the global default; otherwise the request falls back to {@code
   * Constants.baseHttpApiUrl} via {@code GeneralApi}.
   */
  public static GeneralServiceOption newServiceOption(
      HttpMethod method, String path, String baseUrl) {
    GeneralServiceOption opt =
        GeneralServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(method)
            .streamingMode(StreamingMode.OUT)
            .path(path)
            .build();
    if (baseUrl != null) {
      opt.setBaseHttpUrl(baseUrl);
    }
    return opt;
  }

  /**
   * Stamp the client's instance apiKey onto a param if the caller didn't set one. The global
   * fallback chain in {@link com.alibaba.dashscope.utils.ApiKey#getApiKey} still applies when
   * {@code apiKey} is null.
   */
  public static <P extends HalfDuplexParamBase> P withApiKey(String apiKey, P param) {
    if (apiKey != null && param.getApiKey() == null) {
      param.setApiKey(apiKey);
    }
    return param;
  }

  /**
   * Append {@code key=value} (URL-encoded) to {@code sb} if value is non-null. Joins with {@literal
   * &}.
   */
  public static void appendParam(StringBuilder sb, String key, Object value) {
    if (value == null) {
      return;
    }
    if (sb.length() > 0) {
      sb.append("&");
    }
    try {
      sb.append(key).append("=").append(URLEncoder.encode(value.toString(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      sb.append(key).append("=").append(value);
    }
  }
}
