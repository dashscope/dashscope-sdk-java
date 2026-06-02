package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationUsage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class TestMultiModalConversationUsagePlugins {

  private static final Gson GSON = new Gson();

  /**
   * 验证 MultiModalConversationUsage 声明了 plugins 字段（反射检查）。
   */
  @Test
  public void testPluginsFieldDeclared() {
    List<String> fieldNames =
        Arrays.stream(MultiModalConversationUsage.class.getDeclaredFields())
            .map(Field::getName)
            .collect(Collectors.toList());

    assertTrue(fieldNames.contains("plugins"),
        "MultiModalConversationUsage should declare 'plugins' field, actual fields: " + fieldNames);
  }

  /**
   * 验证 plugins 字段的 @SerializedName 值为 "plugins"。
   */
  @Test
  public void testPluginsSerializedName() throws NoSuchFieldException {
    Field pluginsField = MultiModalConversationUsage.class.getDeclaredField("plugins");
    SerializedName annotation = pluginsField.getAnnotation(SerializedName.class);

    assertNotNull(annotation, "plugins field should have @SerializedName annotation");
    assertEquals("plugins", annotation.value());
  }

  /**
   * 验证 getPlugins() 方法存在且可调用。
   */
  @Test
  public void testGetPluginsMethodExists() {
    MultiModalConversationUsage usage = new MultiModalConversationUsage();
    assertDoesNotThrow(usage::getPlugins);
    assertNull(usage.getPlugins(), "plugins should be null when not set");
  }

  /**
   * 模拟服务端返回含 usage.plugins.search 的 JSON，验证 Gson 反序列化后能正确读取。
   */
  @Test
  public void testGsonDeserializeWithPlugins() {
    String json =
        "{"
            + "\"input_tokens\": 1280,"
            + "\"output_tokens\": 256,"
            + "\"total_tokens\": 1536,"
            + "\"image_tokens\": 1024,"
            + "\"plugins\": {"
            + "  \"search\": {"
            + "    \"count\": 2,"
            + "    \"strategy\": \"web_search\""
            + "  }"
            + "}"
            + "}";

    MultiModalConversationUsage usage = GSON.fromJson(json, MultiModalConversationUsage.class);

    assertEquals(1280, (int) usage.getInputTokens());
    assertEquals(256, (int) usage.getOutputTokens());
    assertEquals(1536, (int) usage.getTotalTokens());
    assertEquals(1024, (int) usage.getImageTokens());

    assertNotNull(usage.getPlugins(), "plugins should not be null after deserialization");
    assertNotNull(usage.getPlugins().getSearch(), "plugins.search should not be null");
    assertEquals(2, (int) usage.getPlugins().getSearch().getCount());
    assertEquals("web_search", usage.getPlugins().getSearch().getStrategy());
  }

  /**
   * 验证不含 plugins 的旧版 JSON 仍能正常反序列化（向后兼容）。
   */
  @Test
  public void testGsonDeserializeWithoutPlugins() {
    String json =
        "{"
            + "\"input_tokens\": 100,"
            + "\"output_tokens\": 50,"
            + "\"total_tokens\": 150"
            + "}";

    MultiModalConversationUsage usage = GSON.fromJson(json, MultiModalConversationUsage.class);

    assertEquals(100, (int) usage.getInputTokens());
    assertEquals(50, (int) usage.getOutputTokens());
    assertEquals(150, (int) usage.getTotalTokens());
    assertNull(usage.getPlugins(), "plugins should be null when not present in JSON");
  }

  /**
   * 验证 plugins.search 部分字段缺失时不会报错。
   */
  @Test
  public void testGsonDeserializeWithPartialPlugins() {
    String json =
        "{"
            + "\"input_tokens\": 100,"
            + "\"output_tokens\": 50,"
            + "\"total_tokens\": 150,"
            + "\"plugins\": {"
            + "  \"search\": {"
            + "    \"count\": 5"
            + "  }"
            + "}"
            + "}";

    MultiModalConversationUsage usage = GSON.fromJson(json, MultiModalConversationUsage.class);

    assertNotNull(usage.getPlugins());
    assertNotNull(usage.getPlugins().getSearch());
    assertEquals(5, (int) usage.getPlugins().getSearch().getCount());
    assertNull(usage.getPlugins().getSearch().getStrategy(),
        "strategy should be null when not present");
  }
}
