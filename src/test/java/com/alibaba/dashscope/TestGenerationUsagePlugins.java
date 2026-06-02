package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.dashscope.aigc.generation.GenerationUsage;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class TestGenerationUsagePlugins {

  private static final Gson GSON = new Gson();

  /**
   * 验证 GenerationUsage 声明了 plugins 字段（反射检查，对应客户 dump 逻辑）。
   */
  @Test
  public void testPluginsFieldDeclared() {
    List<String> fieldNames =
        Arrays.stream(GenerationUsage.class.getDeclaredFields())
            .map(Field::getName)
            .collect(Collectors.toList());

    assertTrue(fieldNames.contains("plugins"),
        "GenerationUsage should declare 'plugins' field, actual fields: " + fieldNames);
  }

  /**
   * 验证 plugins 字段的 @SerializedName 值为 "plugins"。
   */
  @Test
  public void testPluginsSerializedName() throws NoSuchFieldException {
    Field pluginsField = GenerationUsage.class.getDeclaredField("plugins");
    SerializedName annotation = pluginsField.getAnnotation(SerializedName.class);

    assertNotNull(annotation, "plugins field should have @SerializedName annotation");
    assertEquals("plugins", annotation.value());
  }

  /**
   * 验证 getPlugins() 方法存在且可调用。
   */
  @Test
  public void testGetPluginsMethodExists() {
    GenerationUsage usage = GenerationUsage.builder().build();
    // 不抛异常即说明 getPlugins() 方法存在
    assertDoesNotThrow(usage::getPlugins);
    assertNull(usage.getPlugins(), "plugins should be null when not set");
  }

  /**
   * 模拟服务端返回含 usage.plugins.search 的 JSON，验证 Gson 反序列化后能正确读取。
   * 对应客户真实调用场景：request_id 541336c2-... 返回的 JSON 结构。
   */
  @Test
  public void testGsonDeserializeWithPlugins() {
    String json =
        "{"
            + "\"input_tokens\": 3550,"
            + "\"output_tokens\": 382,"
            + "\"total_tokens\": 3932,"
            + "\"plugins\": {"
            + "  \"search\": {"
            + "    \"count\": 3,"
            + "    \"strategy\": \"web_search\""
            + "  }"
            + "}"
            + "}";

    GenerationUsage usage = GSON.fromJson(json, GenerationUsage.class);

    assertEquals(3550, (int) usage.getInputTokens());
    assertEquals(382, (int) usage.getOutputTokens());
    assertEquals(3932, (int) usage.getTotalTokens());

    assertNotNull(usage.getPlugins(), "plugins should not be null after deserialization");
    assertNotNull(usage.getPlugins().getSearch(), "plugins.search should not be null");
    assertEquals(3, (int) usage.getPlugins().getSearch().getCount());
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

    GenerationUsage usage = GSON.fromJson(json, GenerationUsage.class);

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

    GenerationUsage usage = GSON.fromJson(json, GenerationUsage.class);

    assertNotNull(usage.getPlugins());
    assertNotNull(usage.getPlugins().getSearch());
    assertEquals(5, (int) usage.getPlugins().getSearch().getCount());
    assertNull(usage.getPlugins().getSearch().getStrategy(),
        "strategy should be null when not present");
  }
}
