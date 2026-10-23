// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.common.Message;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestJson {

  @Data
  @AllArgsConstructor
  @EqualsAndHashCode
  static class InnerClass {

    @SerializedName("test_inner_string")
    private String testInnerString;
  }

  @Data
  @AllArgsConstructor
  @EqualsAndHashCode
  static class TestClass {
    private String testString;
    private int testInt;
    private double testDouble;
    private boolean testBool;
    private InnerClass innerClass;
  }

  @Test
  public void testParseJson() {

    TestClass testIns = new TestClass("test1", 0, 0.1, false, new InnerClass("test2"));

    String serialized = JsonUtils.toJson(testIns);

    assert (serialized.contains("test_inner_string"));

    JsonObject jsonObject = JsonUtils.parse(serialized);
    JsonObject jsonObject2 = JsonUtils.toJsonObject(testIns);

    assert (jsonObject
        .getAsJsonObject("innerClass")
        .get("test_inner_string")
        .getAsString()
        .equals("test2"));
    assert (jsonObject.equals(jsonObject2));

    TestClass testIns2 = JsonUtils.fromJson(serialized, TestClass.class);

    assert (testIns2.equals(testIns));
  }

  @Test
  public void testMessageSerializeDeserialize() {
    Message msg = Message.builder().role("user").content("hello world").build();
    String msgJson = JsonUtils.toJson(msg);
    System.out.println(msgJson);
    Message deserializeMsg = JsonUtils.fromJson(msgJson, Message.class);
    System.out.println(deserializeMsg);
  }
}
