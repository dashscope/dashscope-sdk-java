// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Canned DashScope backend for the examples dry-run. Routes every endpoint the examples touch
 * (HTTP, SSE and both WebSocket protocols) to minimal success responses cribbed from the SDK's
 * own mock tests.
 */
public final class DryRunDispatcher extends Dispatcher {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String JSON = "application/json";
  private static final String SSE = "text/event-stream";

  private final MockWebServer server;

  public DryRunDispatcher(MockWebServer server) {
    this.server = server;
  }

  @Override
  public @NotNull MockResponse dispatch(@NotNull RecordedRequest request) {
    String path = request.getPath() == null ? "/" : request.getPath();
    String method = request.getMethod();
    String body = request.getBody().readUtf8();
    try {
      return route(method, path, request, body);
    } catch (Exception e) {
      return json(500, "{\"code\":\"DryRunError\",\"message\":\"" + e.getMessage() + "\"}");
    }
  }

  private MockResponse route(String method, String path, RecordedRequest request, String body)
      throws Exception {
    // WebSocket endpoints.
    if (path.startsWith("/api-ws/v1/inference")) {
      return MockWs.upgrade(new MockWs.TaskSocket());
    }
    if (path.startsWith("/api-ws/v1/realtime")) {
      HttpUrl url = request.getRequestUrl();
      return MockWs.upgrade(new MockWs.RealtimeSocket(url == null ? null : url.queryParameter("model")));
    }
    // AgentStudio (2.x shared service) lives under its own base URL.
    if (path.startsWith("/api/v1/agentstudio/")) {
      return agentStudio(method, path.substring("/api/v1/agentstudio/".length()));
    }
    // OSS policy + upload for local-file multimodal inputs.
    if (path.startsWith("/uploads")) {
      return json(
          200,
          "{\"data\":{\"upload_host\":\""
              + server.url("/oss/")
              + "\",\"oss_access_key_id\":\"ak\",\"signature\":\"sig\",\"policy\":\"pol\","
              + "\"upload_dir\":\"dir/mm\",\"x_oss_object_acl\":\"private\","
              + "\"x_oss_forbid_overwrite\":\"true\"}}");
    }
    if (path.startsWith("/oss")) {
      return new MockResponse().setResponseCode(200);
    }
    switch (path) {
      case "/services/aigc/text-generation/generation":
        return isSse(request) ? sse(chatDeltaFrames()) : json(200, CHAT_JSON);
      case "/services/aigc/multimodal-generation/generation":
        return multimodal(request, body);
      case "/services/embeddings/text-embedding/text-embedding":
        if (isAsync(request)) {
          return jobCreate("dry-task");
        }
        return json(200, embeddingsJson(body));
      case "/services/embeddings/multimodal-embedding/multimodal-embedding":
        return json(
            200,
            "{\"output\":{\"embeddings\":[{\"index\":0,\"type\":\"text\",\"embedding\":[0.1,0.2]}]},"
                + "\"usage\":{\"input_tokens\":2,\"image_count\":0},\"request_id\":\"me-1\"}");
      case "/services/rerank/text-rerank/text-rerank":
        return json(
            200,
            "{\"output\":{\"results\":[{\"index\":1,\"relevance_score\":0.9,"
                + "\"document\":{\"text\":\"doc-b\"}},{\"index\":0,\"relevance_score\":0.1,"
                + "\"document\":{\"text\":\"doc-a\"}}]},\"usage\":{\"total_tokens\":10},"
                + "\"request_id\":\"rr1\"}");
      case "/services/aigc/text2image/image-synthesis":
      case "/services/aigc/image-generation/generation":
      case "/services/aigc/video-generation/video-synthesis":
      case "/services/audio/asr/transcription":
        return jobCreate("dry-task");
      case "/services/audio/tts/SpeechSynthesizer":
        return sse(ttsAudioFrames());
      case "/services/audio/tts/customization":
        return voiceCustomization(body);
      case "/services/audio/asr/customization":
        return json(200, "{\"output\":{\"vocabulary_id\":\"vocab-dry\"},\"request_id\":\"r1\"}");
      case "/tokenizer":
        return json(
            200,
            "{\"output\":{\"token_ids\":[2610,525,264],\"tokens\":[\"You\",\" are\",\" helpful\"]},"
                + "\"usage\":{\"input_tokens\":3},\"request_id\":\"tk1\"}");
      case "/systemone":
      case "/compatible-mode/v1/systemone":
        return json(200, DECISION_JSON);
      default:
        break;
    }
    if (path.startsWith("/apps/") && path.endsWith("/completion")) {
      return isSse(request) ? sse(appDeltaFrames()) : json(200, APP_JSON);
    }
    if (path.equals("/tasks")) {
      return json(
          200,
          "{\"output\":{\"data\":[{\"task_id\":\"dry-task\",\"task_status\":\"SUCCEEDED\"}],"
              + "\"total\":1,\"page_no\":1,\"page_size\":10},\"request_id\":\"r-list\"}");
    }
    if (path.startsWith("/tasks/")) {
      String taskId = path.substring("/tasks/".length());
      int slash = taskId.indexOf('/');
      if (slash > 0) {
        taskId = taskId.substring(0, slash);
      }
      return json(200, taskJson(taskId));
    }
    if (path.equals("/files") && "POST".equals(method)) {
      return json(
          200,
          "{\"data\":{\"uploaded_files\":[{\"file_id\":\"file-dry\",\"name\":\"dry.jsonl\","
              + "\"purpose\":\"fine-tune\",\"size\":16,\"uploaded_at\":\"2026-10-22 10:00:00\"}]},"
              + "\"request_id\":\"r1\"}");
    }
    if (path.startsWith("/files?")) {
      return json(
          200,
          "{\"data\":{\"files\":[{\"file_id\":\"file-dry\",\"purpose\":\"fine-tune\"}],"
              + "\"page_no\":1,\"page_size\":10,\"total\":1},\"request_id\":\"r2\"}");
    }
    if (path.startsWith("/files/")) {
      if ("DELETE".equals(method)) {
        return json(200, "{\"data\":{\"deleted\":true},\"request_id\":\"r4\"}");
      }
      return json(
          200,
          "{\"data\":{\"file_id\":\"file-dry\",\"name\":\"dry.jsonl\",\"size\":16,"
              + "\"upload_time\":\"2026-10-01\"},\"request_id\":\"r3\"}");
    }
    if (path.startsWith("/models")) {
      HttpUrl url = request.getRequestUrl();
      if (url != null && url.queryParameter("model") != null) {
        return json(
            200,
            "{\"output\":{\"models\":[{\"model\":\"qwen3.8-max\",\"provider\":\"Alibaba\"}]},"
                + "\"request_id\":\"r2\"}");
      }
      return json(
          200,
          "{\"output\":{\"models\":[{\"model\":\"qwen3.8-max\",\"provider\":\"Alibaba\","
              + "\"opensource\":false}],\"page_no\":1,\"page_size\":10,\"total\":1},"
              + "\"request_id\":\"r1\"}");
    }
    if (path.startsWith("/fine-tunes")) {
      if (path.equals("/fine-tunes") || path.startsWith("/fine-tunes?")) {
        return json(
            200,
            "{\"output\":{\"jobs\":[{\"job_id\":\"ft-dry\",\"status\":\"SUCCEEDED\"}],"
                + "\"page_no\":1,\"page_size\":10,\"total\":1},\"request_id\":\"r3\"}");
      }
      return json(
          200,
          "{\"output\":{\"job_id\":\"ft-dry\",\"status\":\"SUCCEEDED\",\"finetuned_output\":\"m\","
              + "\"create_time\":\"2026-10-01\"},\"request_id\":\"r2\"}");
    }
    return json(404, "{\"code\":\"NotFound\",\"message\":\"dry-run has no route for " + path + "\"}");
  }

  private MockResponse multimodal(RecordedRequest request, String body) throws Exception {
    if (isAsync(request)) {
      String model = MAPPER.readTree(body).path("model").asText("");
      return jobCreate(model.startsWith("tingwu") ? "dry-tw" : "dry-task");
    }
    if (isSse(request)) {
      String model = MAPPER.readTree(body).path("model").asText("");
      if (model.contains("tts")) {
        return sse(ttsAudioFrames());
      }
      return sse(
          "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
              + "\"content\":[{\"text\":\"你\"}]}}]},\"request_id\":\"m2\"}\n\n"
              + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":[{\"text\":\"你好\"}]},"
              + "\"finish_reason\":\"stop\"}]},\"usage\":{\"input_tokens\":10,\"output_tokens\":5,"
              + "\"total_tokens\":15},\"request_id\":\"m2\"}\n\n");
    }
    return json(
        200,
        "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\","
            + "\"content\":[{\"text\":\"一只猫\"},{\"text\":\"在睡觉\"}]}}]},\"usage\":{\"input_tokens\":10,"
            + "\"output_tokens\":5,\"total_tokens\":15,\"image_tokens\":8},\"request_id\":\"mm-1\"}");
  }

  private MockResponse voiceCustomization(String body) throws Exception {
    String action = MAPPER.readTree(body).path("input").path("action").asText("");
    switch (action) {
      case "create_voice":
      case "query_voice":
        return json(
            200,
            "{\"output\":{\"voice_id\":\"voice-dry\",\"status\":\"OK\",\"prefix\":\"dry\"},"
                + "\"request_id\":\"r3\"}");
      case "list_voice":
        return json(
            200,
            "{\"output\":{\"voice_list\":[{\"voice_id\":\"voice-dry\",\"prefix\":\"dry\"}]},"
                + "\"request_id\":\"r2\"}");
      case "delete_voice":
      case "update_voice":
        return json(200, "{\"output\":{\"status\":\"SUCCEEDED\"},\"request_id\":\"r4\"}");
      default:
        return json(400, "{\"code\":\"InvalidParameter\",\"message\":\"unknown action " + action + "\"}");
    }
  }

  private MockResponse agentStudio(String method, String resource) {
    if (resource.equals("environments") && "POST".equals(method)) {
      return json(
          200,
          "{\"id\":\"env-dry\",\"type\":\"environment\",\"name\":\"quickstart-env\","
              + "\"request_id\":\"r-e1\"}");
    }
    if (resource.equals("agents") && "POST".equals(method)) {
      return json(
          200,
          "{\"id\":\"agent-dry\",\"type\":\"agent\",\"name\":\"quickstart-agent\","
              + "\"request_id\":\"r-a1\"}");
    }
    if (resource.equals("sessions") && "POST".equals(method)) {
      return json(200, "{\"id\":\"sess-dry\",\"type\":\"session\",\"request_id\":\"r-s1\"}");
    }
    if (resource.matches("sessions/[^/]+/events") && "POST".equals(method)) {
      return json(200, "{\"id\":\"evt-dry\",\"request_id\":\"r-s2\"}");
    }
    if (resource.matches("sessions/[^/]+/events/stream.*")) {
      return sse(
          "data: {\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"text\","
              + "\"text\":\"西湖的断桥其实不断。\"}]}\n\n"
              + "data: {\"type\":\"session_status\",\"content\":[{\"type\":\"data\","
              + "\"data\":{\"session_status\":\"idle\"}}]}\n\n");
    }
    if (resource.matches("sessions/[^/]+/archive")) {
      return json(200, "{\"id\":\"sess-dry\",\"request_id\":\"r-s3\"}");
    }
    if (resource.equals("deployments") && "POST".equals(method)) {
      return json(
          200,
          "{\"id\":\"dep-dry\",\"name\":\"daily-summary\",\"status\":\"READY\",\"requestId\":\"r-d1\"}");
    }
    if (resource.matches("deployments/[^/]+/run")) {
      return json(
          200,
          "{\"id\":\"run-dry\",\"status\":\"RUNNING\",\"trigger_source\":\"manual\","
              + "\"requestId\":\"r-d2\"}");
    }
    if (resource.matches("deployments/[^/]+/runs.*")) {
      return json(
          200,
          "{\"data\":[{\"id\":\"run-dry\",\"status\":\"SUCCEEDED\",\"trigger_source\":\"manual\"}],"
              + "\"next_page\":null,\"requestId\":\"r-d3\"}");
    }
    return json(404, "{\"code\":\"NotFound\",\"message\":\"dry-run agentstudio: " + resource + "\"}");
  }

  private static MockResponse jobCreate(String taskId) {
    return json(
        200,
        "{\"output\":{\"task_id\":\"" + taskId + "\",\"task_status\":\"PENDING\"},"
            + "\"request_id\":\"r-submit\"}");
  }

  private static String taskJson(String taskId) {
    if ("dry-tw".equals(taskId)) {
      return "{\"output\":{\"task_id\":\"dry-tw\",\"task_status\":\"SUCCEEDED\",\"TaskId\":\"dry-tw\","
          + "\"Result\":\"{\\\"summary\\\":\\\"ok\\\"}\"},\"request_id\":\"r-fetch\"}";
    }
    return "{\"output\":{\"task_id\":\""
        + taskId
        + "\",\"task_status\":\"SUCCEEDED\","
        + "\"results\":[{\"url\":\"https://img.example/1.png\"},{\"url\":\"https://img.example/2.png\"}],"
        + "\"video_url\":\"https://vid.example/1.mp4\",\"url\":\"https://files.example/result.txt\"},"
        + "\"usage\":{\"input_tokens\":10},\"request_id\":\"r-fetch\"}";
  }

  private static String embeddingsJson(String body) throws Exception {
    JsonNode texts = MAPPER.readTree(body).path("input").path("texts");
    int count = texts.isArray() ? texts.size() : 1;
    ObjectNode root = MAPPER.createObjectNode();
    ArrayNode embeddings = root.putObject("output").putArray("embeddings");
    for (int i = 0; i < count; i++) {
      ObjectNode item = embeddings.addObject();
      item.put("text_index", i);
      item.putArray("embedding").add(0.1).add(0.2).add(0.3);
    }
    root.putObject("usage").put("input_tokens", count).put("total_tokens", count);
    root.put("request_id", "e1");
    return MAPPER.writeValueAsString(root);
  }

  private static boolean isSse(RecordedRequest request) {
    String header = request.getHeader("X-DashScope-SSE");
    return "enable".equalsIgnoreCase(header == null ? "" : header);
  }

  private static boolean isAsync(RecordedRequest request) {
    String header = request.getHeader("X-DashScope-Async");
    return "enable".equalsIgnoreCase(header == null ? "" : header);
  }

  private static MockResponse json(int code, String body) {
    return new MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", JSON)
        .setBody(body);
  }

  private static MockResponse sse(String frames) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", SSE)
        .setBody(frames);
  }

  private static String chatDeltaFrames() {
    return "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"你\"}}]},"
        + "\"request_id\":\"s1\"}\n\n"
        + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你好\"},"
        + "\"finish_reason\":\"stop\"}]},\"usage\":{\"input_tokens\":1,\"output_tokens\":2,"
        + "\"total_tokens\":3},\"request_id\":\"s1\"}\n\n";
  }

  private static String appDeltaFrames() {
    return "data: {\"output\":{\"text\":\"你\",\"session_id\":\"s1\"},\"request_id\":\"rs1\"}\n\n"
        + "data: {\"output\":{\"text\":\"你好\",\"finish_reason\":\"stop\"},\"usage\":{\"input_tokens\":1,"
        + "\"output_tokens\":2,\"total_tokens\":3},\"request_id\":\"rs1\"}\n\n";
  }

  private static String ttsAudioFrames() {
    String chunk1 = java.util.Base64.getEncoder().encodeToString(MockWs.pcmChunk(2400));
    String chunk2 = java.util.Base64.getEncoder().encodeToString(MockWs.pcmChunk(2400));
    return "data: {\"output\":{\"audio\":\"" + chunk1 + "\"}}\n\n"
        + "data: {\"output\":{\"audio\":\"" + chunk2 + "\"},\"usage\":{\"input_tokens\":2}}\n\n"
        + "data: [DONE]\n\n";
  }

  private static final String CHAT_JSON =
      "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\","
          + "\"content\":\"你好\"}}]},\"usage\":{\"input_tokens\":10,\"output_tokens\":5,"
          + "\"total_tokens\":15},\"request_id\":\"req-abc\"}";

  private static final String APP_JSON =
      "{\"output\":{\"text\":\"答案\",\"finish_reason\":\"stop\",\"session_id\":\"sess-1\"},"
          + "\"usage\":{\"input_tokens\":10,\"output_tokens\":5,\"total_tokens\":15},"
          + "\"request_id\":\"req-app-1\"}";

  private static final String DECISION_JSON =
      "{\"model\":\"decision-model-preview\",\"answers\":{"
          + "\"department\":{\"type\":\"choice\",\"choice\":\"technical\",\"confidence\":0.87,"
          + "\"probabilities\":{\"billing\":0.13,\"technical\":0.87}},"
          + "\"escalate\":{\"type\":\"noul\",\"noul\":0.05},"
          + "\"severity\":{\"type\":\"score\",\"score\":1.43,\"confidence\":0.62,"
          + "\"probabilities\":{\"0\":0.1,\"1\":0.6,\"2\":0.3},"
          + "\"legend\":{\"0\":\"low\",\"1\":\"mid\",\"2\":\"high\"}}},"
          + "\"usage\":{\"input_tokens\":128},\"latency_ms\":35.2,\"request_id\":\"req-dec-1\"}";
}
