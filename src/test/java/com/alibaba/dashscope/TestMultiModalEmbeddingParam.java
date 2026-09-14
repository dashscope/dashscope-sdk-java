package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemImage;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemText;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingParam;
import com.google.gson.JsonObject;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class TestMultiModalEmbeddingParam {

  private MultiModalEmbeddingParam.MultiModalEmbeddingParamBuilder<?, ?> baseBuilder() {
    return MultiModalEmbeddingParam.builder()
        .model("tongyi-embedding-vision-plus")
        .apiKey("test-key")
        .contents(
            Arrays.asList(
                new MultiModalEmbeddingItemImage("https://example.com/a.jpg"),
                new MultiModalEmbeddingItemText("hello")));
  }

  @Test
  public void testDimensionSerializedIntoParameters() {
    MultiModalEmbeddingParam param = baseBuilder().dimension(1152).build();
    JsonObject body = param.getHttpBody();
    assertTrue(body.has("parameters"));
    assertEquals(1152, body.getAsJsonObject("parameters").get("dimension").getAsInt());
  }

  @Test
  public void testAllExplicitParamsSerialized() {
    MultiModalEmbeddingParam param =
        baseBuilder()
            .dimension(1024)
            .outputType("dense")
            .fps(0.5)
            .instruct("retrieve images")
            .enableFusion(true)
            .resLevel(1)
            .maxVideoFrames(16)
            .build();
    JsonObject params = param.getHttpBody().getAsJsonObject("parameters");
    assertEquals(1024, params.get("dimension").getAsInt());
    assertEquals("dense", params.get("output_type").getAsString());
    assertEquals(0.5, params.get("fps").getAsDouble(), 1e-9);
    assertEquals("retrieve images", params.get("instruct").getAsString());
    assertTrue(params.get("enable_fusion").getAsBoolean());
    assertEquals(1, params.get("res_level").getAsInt());
    assertEquals(16, params.get("max_video_frames").getAsInt());
  }

  @Test
  public void testGenericParameterStillWorks() {
    // 旧用法：通用 parameters 通道
    MultiModalEmbeddingParam param = baseBuilder().parameter("dimension", 768).build();
    JsonObject params = param.getHttpBody().getAsJsonObject("parameters");
    assertEquals(768, params.get("dimension").getAsInt());
  }

  @Test
  public void testNoExplicitParamsNoDimensionKey() {
    MultiModalEmbeddingParam param = baseBuilder().build();
    JsonObject body = param.getHttpBody();
    if (body.has("parameters")) {
      assertFalse(body.getAsJsonObject("parameters").has("dimension"));
    }
    // contents 始终在 input 中
    assertEquals(2, body.getAsJsonObject("input").getAsJsonArray("contents").size());
  }
}
