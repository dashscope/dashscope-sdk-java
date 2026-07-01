// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public abstract class ContentBlock {
  @SerializedName("type")
  private String type;

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Text extends ContentBlock {
    @SerializedName("text")
    private String text;

    @SerializedName("citations")
    private List<Citation> citations;
  }

  @Data
  public static class Citation {
    @SerializedName("url")
    private String url;

    @SerializedName("title")
    private String title;

    @SerializedName("cited_text")
    private String citedText;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Image extends ContentBlock {
    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("file_id")
    private String fileId;

    @SerializedName("image_data")
    private String imageData;

    @SerializedName("media_type")
    private String mediaType;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Audio extends ContentBlock {
    @SerializedName("data")
    private String data;

    @SerializedName("format")
    private String format;

    @SerializedName("file_id")
    private String fileId;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class DataContent extends ContentBlock {
    @SerializedName("data")
    private JsonObject data;

    @SerializedName("name")
    private String name;

    @SerializedName("title")
    private String title;

    @SerializedName("context")
    private String context;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class File extends ContentBlock {
    @SerializedName("file_url")
    private String fileUrl;

    @SerializedName("file_id")
    private String fileId;

    @SerializedName("file_data")
    private String fileData;

    @SerializedName("media_type")
    private String mediaType;

    @SerializedName("filename")
    private String filename;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Refusal extends ContentBlock {
    @SerializedName("refusal")
    private String refusal;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Error extends ContentBlock {
    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("message")
    private String message;
  }

  public static class Deserializer implements JsonDeserializer<ContentBlock> {
    private static final Map<String, Class<? extends ContentBlock>> REGISTRY = new HashMap<>();
    private static final Gson PLAIN_GSON = new GsonBuilder().create();

    static {
      REGISTRY.put("text", Text.class);
      REGISTRY.put("image", Image.class);
      REGISTRY.put("audio", Audio.class);
      REGISTRY.put("data", DataContent.class);
      REGISTRY.put("file", File.class);
      REGISTRY.put("refusal", Refusal.class);
      REGISTRY.put("error", Error.class);
    }

    @Override
    public ContentBlock deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (!json.isJsonObject()) {
        return null;
      }
      JsonObject obj = json.getAsJsonObject();
      String type = null;
      if (obj.has("type") && obj.get("type").isJsonPrimitive()) {
        type = obj.get("type").getAsString();
      }
      Class<? extends ContentBlock> clazz = type != null ? REGISTRY.get(type) : null;
      if (clazz == null) {
        clazz = DataContent.class;
      }
      return PLAIN_GSON.fromJson(obj, clazz);
    }
  }
}
