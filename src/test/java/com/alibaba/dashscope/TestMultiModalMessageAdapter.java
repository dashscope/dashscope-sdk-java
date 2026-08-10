// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestMultiModalMessageAdapter {

  private static class CustomContent {
    public String url = "http://example.com/a.png";
  }

  @Test
  public void testBigDecimalAndBigIntegerViaToJsonTree() {
    Map<String, Object> videoContent = new HashMap<>();
    videoContent.put("video", Arrays.asList("base64img1", "base64img2"));
    videoContent.put("fps", new BigDecimal("0.1"));
    videoContent.put("nframes", new BigInteger("12345678901234567890"));

    MultiModalMessage msg =
        MultiModalMessage.builder()
            .role("user")
            .content(
                Arrays.asList(
                    Collections.singletonMap("text", (Object) "describe this video"), videoContent))
            .build();

    // MultiModalConversationParam.getInput() serializes through Gson.toJsonTree,
    // whose JsonTreeWriter does not support jsonValue()
    JsonArray array = JsonUtils.toJsonArray(Arrays.asList(msg));
    JsonObject video =
        array.get(0).getAsJsonObject().getAsJsonArray("content").get(1).getAsJsonObject();
    assertTrue(video.get("fps").isJsonPrimitive());
    assertTrue(video.get("fps").getAsJsonPrimitive().isNumber());
    assertEquals(new BigDecimal("0.1"), video.get("fps").getAsBigDecimal());
    assertEquals(new BigInteger("12345678901234567890"), video.get("nframes").getAsBigInteger());
  }

  @Test
  public void testUnsupportedTypeFallbackViaToJsonTree() {
    Map<String, Object> content = new HashMap<>();
    content.put("custom", new CustomContent());

    MultiModalMessage msg =
        MultiModalMessage.builder().role("user").content(Arrays.asList(content)).build();

    JsonArray array = JsonUtils.toJsonArray(Arrays.asList(msg));
    JsonObject custom =
        array
            .get(0)
            .getAsJsonObject()
            .getAsJsonArray("content")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("custom");
    assertEquals("http://example.com/a.png", custom.get("url").getAsString());
  }

  @Test
  public void testStreamingWriterPathStillWorks() {
    Map<String, Object> content = new HashMap<>();
    content.put("fps", new BigDecimal("0.1"));
    content.put("custom", new CustomContent());

    MultiModalMessage msg =
        MultiModalMessage.builder().role("user").content(Arrays.asList(content)).build();

    String json = JsonUtils.toJson(msg);
    JsonObject parsed = JsonUtils.parse(json);
    JsonObject first = parsed.getAsJsonArray("content").get(0).getAsJsonObject();
    assertEquals(new BigDecimal("0.1"), first.get("fps").getAsBigDecimal());
    assertEquals(
        "http://example.com/a.png", first.getAsJsonObject("custom").get("url").getAsString());
  }
}
