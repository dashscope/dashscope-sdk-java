// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.dashscope.audio.live.*;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestQwenLiveSessionConfig {
  private QwenLiveDelegation client() {
    return QwenLiveDelegation.builder().type(QwenLiveDelegationType.CLIENT).build();
  }

  @Test
  void clientConfigOmitsUnsetKeys() {
    assertEquals(
        JsonUtils.parse("{\"model\":\"front\",\"delegation\":{\"type\":\"client\"}}"),
        QwenLiveSessionConfig.builder().model("front").delegation(client()).build().getConfig());
  }

  @Test
  void responsesAreNested() {
    JsonObject actual =
        QwenLiveSessionConfig.builder()
            .model("front")
            .delegation(
                QwenLiveDelegation.builder()
                    .type(QwenLiveDelegationType.RESPONSES)
                    .responses(
                        QwenLiveResponsesConfig.builder()
                            .model("backend-test")
                            .instructions("处理任务")
                            .tools(Collections.emptyList())
                            .build())
                    .build())
            .build()
            .getConfig();
    assertEquals(
        JsonUtils.parse(
            "{\"model\":\"front\",\"delegation\":{\"type\":\"responses\",\"responses\":{"
                + "\"model\":\"backend-test\",\"instructions\":\"处理任务\",\"tools\":[]}}}"),
        actual);
    assertFalse(actual.has("turn_detection"));
    assertFalse(actual.has("delegate"));
  }

  @Test
  void invalidDelegationCombinationsAreRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> QwenLiveSessionConfig.builder().build().getConfig());
    assertThrows(
        IllegalArgumentException.class, () -> QwenLiveDelegation.builder().build().getConfig());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            QwenLiveDelegation.builder()
                .type(QwenLiveDelegationType.RESPONSES)
                .build()
                .getConfig());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            QwenLiveDelegation.builder()
                .type(QwenLiveDelegationType.CLIENT)
                .responses(
                    QwenLiveResponsesConfig.builder().model("backend").instructions("任务").build())
                .build()
                .getConfig());
  }

  @Test
  void backendModelAndInstructionsAreRequired() {
    for (String bad : new String[] {null, "", "  "}) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              QwenLiveResponsesConfig.builder().model(bad).instructions("任务").build().getConfig());
      assertThrows(
          IllegalArgumentException.class,
          () ->
              QwenLiveResponsesConfig.builder()
                  .model("backend")
                  .instructions(bad)
                  .build()
                  .getConfig());
    }
  }

  @Test
  void typedFieldsAndToolsHaveExactWireNames() {
    JsonObject tool =
        JsonUtils.parse(
            "{\"type\":\"function\",\"function\":{\"name\":\"weather\","
                + "\"parameters\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}}}");
    JsonObject original = tool.deepCopy();
    JsonObject actual =
        QwenLiveSessionConfig.builder()
            .model("front")
            .instructions("前台")
            .voice("voice")
            .inputAudioFormat("pcm16")
            .outputAudioFormat("pcm24")
            .modalities(Arrays.asList("audio", "text"))
            .tools(Collections.singletonList(tool))
            .delegation(client())
            .build()
            .getConfig();
    assertEquals("front", actual.get("model").getAsString());
    assertEquals("前台", actual.get("instructions").getAsString());
    assertEquals("voice", actual.get("voice").getAsString());
    assertEquals("pcm16", actual.get("input_audio_format").getAsString());
    assertEquals("pcm24", actual.get("output_audio_format").getAsString());
    assertEquals(JsonUtils.parseString("[\"audio\",\"text\"]"), actual.get("modalities"));
    assertEquals(tool, actual.getAsJsonArray("tools").get(0));
    actual.getAsJsonArray("tools").get(0).getAsJsonObject().addProperty("changed", true);
    assertEquals(original, tool);
    assertFalse(actual.has("inputAudioFormat"));
  }

  @Test
  void topLevelReservedKeysCannotBeInjectedEvenWhenUnset() {
    Map<String, Object> extra = new LinkedHashMap<>();
    for (String key :
        new String[] {
          "model",
          "instructions",
          "voice",
          "input_audio_format",
          "output_audio_format",
          "modalities",
          "tools",
          "delegation",
          "transport",
          "context"
        }) {
      extra.put(key, "injected");
    }
    extra.put("turn_detection", JsonUtils.parse("{\"type\":\"server_vad\"}"));
    extra.put("input_audio_transcription", JsonUtils.parse("{\"model\":\"asr\"}"));
    extra.put("custom", "扩展");
    Map<String, Object> original = new LinkedHashMap<>(extra);
    JsonObject actual =
        QwenLiveSessionConfig.builder()
            .model("front")
            .delegation(client())
            .parameters(extra)
            .build()
            .getConfig();
    assertEquals(5, actual.size());
    assertEquals("front", actual.get("model").getAsString());
    assertEquals("client", actual.getAsJsonObject("delegation").get("type").getAsString());
    assertEquals("扩展", actual.get("custom").getAsString());
    assertTrue(actual.has("turn_detection"));
    assertTrue(actual.has("input_audio_transcription"));
    assertFalse(actual.has("context"));
    assertFalse(actual.has("parameters"));
    assertEquals(original, extra);
  }

  @Test
  void backendExtensionsCannotOverrideReservedKeys() {
    Map<String, Object> extra = new LinkedHashMap<>();
    extra.put("model", "bad");
    extra.put("instructions", "bad");
    extra.put("tools", "bad");
    extra.put("temperature", 0.4);
    JsonObject actual =
        QwenLiveResponsesConfig.builder()
            .model("backend")
            .instructions("任务")
            .parameters(extra)
            .build()
            .getConfig();
    assertEquals("backend", actual.get("model").getAsString());
    assertEquals("任务", actual.get("instructions").getAsString());
    assertFalse(actual.has("tools"));
    assertEquals(0.4, actual.get("temperature").getAsDouble());
    assertFalse(actual.has("parameters"));
    assertEquals(4, extra.size());
  }

  @Test
  void transportIsAnOptionalFlattenedConfiguration() {
    Map<String, Object> extra = new LinkedHashMap<>();
    extra.put("type", "bad");
    extra.put("custom", true);
    JsonObject actual =
        QwenLiveSessionConfig.builder()
            .model("front")
            .delegation(client())
            .transport(QwenLiveTransport.builder().type("aoq").parameters(extra).build())
            .build()
            .getConfig();
    assertEquals(JsonUtils.parse("{\"type\":\"aoq\",\"custom\":true}"), actual.get("transport"));
    assertEquals(
        "websocket",
        QwenLiveTransport.builder()
            .type("websocket")
            .build()
            .getConfig()
            .get("type")
            .getAsString());
    assertThrows(
        IllegalArgumentException.class, () -> QwenLiveTransport.builder().build().getConfig());
    assertThrows(
        IllegalArgumentException.class,
        () -> QwenLiveTransport.builder().type(" ").build().getConfig());
  }

  @Test
  void settersCanUpdateConfiguration() {
    QwenLiveSessionConfig config =
        QwenLiveSessionConfig.builder().model("front").delegation(client()).build();
    config.setVoice("new-voice");
    config.setTools(Collections.emptyList());
    assertEquals("new-voice", config.getConfig().get("voice").getAsString());
    assertEquals(0, config.getConfig().getAsJsonArray("tools").size());
    QwenLiveResponsesConfig backend =
        QwenLiveResponsesConfig.builder().model("b").instructions("任务").build();
    backend.setModel("new-backend");
    QwenLiveDelegation d = client();
    d.setType(QwenLiveDelegationType.RESPONSES);
    d.setResponses(backend);
    config.setDelegation(d);
    QwenLiveTransport transport = QwenLiveTransport.builder().type("aoq").build();
    transport.setType("websocket");
    config.setTransport(transport);
    assertEquals(
        "new-backend",
        config
            .getConfig()
            .getAsJsonObject("delegation")
            .getAsJsonObject("responses")
            .get("model")
            .getAsString());
    assertEquals(
        "websocket", config.getConfig().getAsJsonObject("transport").get("type").getAsString());
  }

  @Test
  void modelIsRequired() {
    for (String bad : new String[] {null, "", "  "}) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              QwenLiveSessionConfig.builder().model(bad).delegation(client()).build().getConfig());
    }
  }

  @Test
  void delegationTypeNoneIsSupported() {
    assertEquals(
        JsonUtils.parse("{\"model\":\"front\",\"delegation\":{\"type\":\"none\"}}"),
        QwenLiveSessionConfig.builder()
            .model("front")
            .delegation(QwenLiveDelegation.builder().type(QwenLiveDelegationType.NONE).build())
            .build()
            .getConfig());
  }

  @Test
  void contextCompactionIsTypedAndNested() {
    JsonObject actual =
        QwenLiveSessionConfig.builder()
            .model("front")
            .delegation(client())
            .context(
                QwenLiveContext.builder()
                    .compaction(
                        QwenLiveContextCompaction.builder().enabled(false).mode("local").build())
                    .build())
            .build()
            .getConfig();
    assertEquals(
        JsonUtils.parse(
            "{\"model\":\"front\",\"context\":{\"compaction\":{\"enabled\":false,\"mode\":\"local\"}},"
                + "\"delegation\":{\"type\":\"client\"}}"),
        actual);
  }

  @Test
  void contextRequiresCompactionWithMode() {
    assertThrows(
        IllegalArgumentException.class, () -> QwenLiveContext.builder().build().getConfig());
    for (String bad : new String[] {null, "", " "}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> QwenLiveContextCompaction.builder().mode(bad).build().getConfig());
    }
  }
}
