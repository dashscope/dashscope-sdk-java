package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AsyncTaskInfo {
  @SerializedName("api_key_id")
  private String apiKeyId;

  @SerializedName("caller_parent_id")
  private String callerParentId;

  @SerializedName("caller_uid")
  private String callerUid;

  @SerializedName("end_time")
  private Long endTime;

  @SerializedName("gmt_create")
  private Long gmtCreate;

  @SerializedName("model_name")
  private String modelName;

  @SerializedName("region")
  private String region;

  @SerializedName("request_id")
  private String requestId;

  @SerializedName("start_time")
  private Long startTime;

  @SerializedName("status")
  private String status;

  @SerializedName("task_id")
  private String taskId;

  @SerializedName("user_api_unique_key")
  private String userApiUniqueKey;
}
