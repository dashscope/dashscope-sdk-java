package com.alibaba.dashscope.legacy_2x.aigc.imagesynthesis;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@Deprecated
public class ImageSynthesisOutput {
  @SerializedName("task_id")
  private String taskId;

  @SerializedName("task_status")
  private String taskStatus;

  private String code;

  private String message;

  /**
   * Task artifacts, usually string members such as {@code url}. Newer models may attach non-string
   * members (objects, arrays, numbers); they are kept as their JSON text so reading a succeeded
   * task never fails deserialization.
   */
  @JsonAdapter(ResultsAdapter.class)
  private List<Map<String, String>> results;

  @SerializedName("task_metrics")
  private ImageSynthesisTaskMetrics taskMetrics;

  /**
   * Lenient {@code results} codec: reads non-string member values as their JSON text instead of
   * throwing; writes the string members back unchanged. Null entries are kept as null, matching the
   * stock map adapter. Both directions are explicit because a deserializer-only adapter leaves Gson
   * 2.8.9 serializing the list as an empty object.
   */
  static final class ResultsAdapter
      implements JsonDeserializer<List<Map<String, String>>>,
          JsonSerializer<List<Map<String, String>>> {
    @Override
    public List<Map<String, String>> deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      if (!json.isJsonArray()) {
        throw new JsonParseException("results must be a JSON array: " + json);
      }
      List<Map<String, String>> results = new ArrayList<>();
      for (JsonElement item : json.getAsJsonArray()) {
        if (item.isJsonNull()) {
          results.add(null);
          continue;
        }
        if (!item.isJsonObject()) {
          throw new JsonParseException("results entries must be JSON objects: " + item);
        }
        JsonObject itemObject = item.getAsJsonObject();
        Map<String, String> entry = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> field : itemObject.entrySet()) {
          JsonElement value = field.getValue();
          String text;
          if (value.isJsonNull()) {
            text = null;
          } else if (value.isJsonPrimitive()) {
            text = value.getAsString();
          } else {
            text = value.toString();
          }
          entry.put(field.getKey(), text);
        }
        results.add(entry);
      }
      return results;
    }

    @Override
    public JsonElement serialize(
        List<Map<String, String>> src, Type typeOfSrc, JsonSerializationContext context) {
      JsonArray array = new JsonArray();
      for (Map<String, String> item : src) {
        if (item == null) {
          array.add(JsonNull.INSTANCE);
          continue;
        }
        JsonObject itemObject = new JsonObject();
        for (Map.Entry<String, String> field : item.entrySet()) {
          itemObject.addProperty(field.getKey(), field.getValue());
        }
        array.add(itemObject);
      }
      return array;
    }
  }
}
