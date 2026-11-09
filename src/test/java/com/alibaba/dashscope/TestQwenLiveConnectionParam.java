// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.audio.live.QwenLiveConnectionParam;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestQwenLiveConnectionParam {

  private static final String DEFAULT_TEMPLATE =
      "wss://{workspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/realtime";

  private String savedTemplate;

  @BeforeEach
  void setUp() {
    savedTemplate = QwenLiveConnectionParam.baseWebsocketApiUrlTemplate;
    QwenLiveConnectionParam.baseWebsocketApiUrlTemplate = DEFAULT_TEMPLATE;
  }

  @AfterEach
  void tearDown() {
    QwenLiveConnectionParam.baseWebsocketApiUrlTemplate = savedTemplate;
  }

  @Test
  void workspaceRoutesToNewGateway() {
    QwenLiveConnectionParam p =
        QwenLiveConnectionParam.builder().model("qwen-live").workspaceId("workspace-test").build();
    assertEquals(
        "wss://workspace-test.cn-beijing.maas.aliyuncs.com/api-ws/v1/realtime?model=qwen-live",
        p.getUrl());
    assertNull(p.getWorkspace());
  }

  @Test
  void templateNeedsWorkspaceId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> QwenLiveConnectionParam.builder().model("qwen-live").build().getUrl());
  }

  @Test
  void explicitUrlDoesNotNeedWorkspaceId() {
    assertEquals(
        "ws://localhost:8080/realtime?model=qwen-live",
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .url("ws://localhost:8080/realtime")
            .build()
            .getUrl());
  }

  @Test
  void documentCasePlaceholderIsAlsoReplaced() {
    QwenLiveConnectionParam.baseWebsocketApiUrlTemplate =
        "wss://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/realtime";
    assertEquals(
        "wss://ws-1.cn-beijing.maas.aliyuncs.com/api-ws/v1/realtime?model=qwen-live",
        QwenLiveConnectionParam.builder().model("qwen-live").workspaceId("ws-1").build().getUrl());
  }

  @Test
  void placeholderInExplicitUrlIsReplaced() {
    assertEquals(
        "wss://ws-2.example.com/realtime?model=qwen-live",
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .workspaceId("ws-2")
            .url("wss://{workspaceId}.example.com/realtime")
            .build()
            .getUrl());
  }

  @Test
  void fixedTemplateDoesNotNeedWorkspaceId() {
    QwenLiveConnectionParam.baseWebsocketApiUrlTemplate = "wss://fixed.example.com/api-ws/v1/realtime";
    assertEquals(
        "wss://fixed.example.com/api-ws/v1/realtime?model=qwen-live",
        QwenLiveConnectionParam.builder().model("qwen-live").build().getUrl());
  }

  @Test
  void existingQueryIsPreserved() {
    assertEquals(
        "ws://localhost:8080/realtime?foo=bar&model=qwen-live",
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .url("ws://localhost:8080/realtime?foo=bar")
            .build()
            .getUrl());
  }

  @Test
  void existingModelQueryIsReplacedOnce() {
    String actual =
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .url("ws://localhost:8080/realtime?model=stale")
            .build()
            .getUrl();
    assertEquals("ws://localhost:8080/realtime?model=qwen-live", actual);
  }

  @Test
  void modelSpecialCharactersAreEncoded() {
    String actual =
        QwenLiveConnectionParam.builder()
            .model("qwen live&x")
            .url("ws://localhost:8080/realtime")
            .build()
            .getUrl();
    assertEquals("ws://localhost:8080/realtime?model=qwen%20live%26x", actual);
  }

  @Test
  void blankModelIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> QwenLiveConnectionParam.builder().model("  ").workspaceId("ws-1").build().getUrl());
  }

  @Test
  void illegalSchemeIsRejected() {
    for (String bad :
        new String[] {"ftp://host/realtime", "host/realtime", "wsss://host/realtime"}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> QwenLiveConnectionParam.builder().model("qwen-live").url(bad).build().getUrl(),
          "should reject url: [" + bad + "]");
    }
  }

  @Test
  void httpSchemeIsAllowedForLocalServer() {
    assertEquals(
        "http://127.0.0.1:9000/realtime?model=qwen-live",
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .url("http://127.0.0.1:9000/realtime")
            .build()
            .getUrl());
  }

  @Test
  void workspaceHeaderIsIndependentFromWorkspaceId() {
    QwenLiveConnectionParam p =
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .workspaceId("ws-host")
            .workspace("llm-xxxx")
            .build();
    assertTrue(p.getUrl().startsWith("wss://ws-host."));
    assertEquals("llm-xxxx", p.getWorkspace());
  }

  @Test
  void headersAreConvertedToStringAndInputIsNotMutated() {
    Map<String, Object> source = new HashMap<>();
    source.put("X-Test", 1);
    QwenLiveConnectionParam p =
        QwenLiveConnectionParam.builder().model("qwen-live").workspaceId("ws-1").headers(source).build();
    Map<String, String> headers = p.getHeaders();
    assertEquals("1", headers.get("X-Test"));
    assertEquals(1, source.size());
    assertEquals(1, source.get("X-Test"));
  }

  @Test
  void emptyHeadersAreSafe() {
    QwenLiveConnectionParam p = QwenLiveConnectionParam.builder().model("qwen-live").workspaceId("ws-1").build();
    assertTrue(p.getHeaders().isEmpty());
  }
}
