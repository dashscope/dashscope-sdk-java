package com.alibaba.dashscope.aigc.multimodalconversation;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class OcrOptions implements Serializable {
  @SerializedName("task")
  private OcrOptions.Task task;

  @SerializedName("task_config")
  private OcrOptions.TaskConfig taskConfig;

  @Getter
  public enum Task {
    @SerializedName("key_information_extraction")
    KEY_INFORMATION_EXTRACTION,

    @SerializedName("text_recognition")
    TEXT_RECOGNITION,

    @SerializedName("table_parsing")
    TABLE_PARSING,

    @SerializedName("document_parsing")
    DOCUMENT_PARSING,

    @SerializedName("formula_recognition")
    FORMULA_RECOGNITION,

    @SerializedName("multi_lan")
    MULTI_LAN,

    @SerializedName("advanced_recognition")
    ADVANCED_RECOGNITION
  }

  @Data
  @SuperBuilder
  public static class TaskConfig {
    @SerializedName("result_schema")
    private JsonObject resultSchema;
  }
}
