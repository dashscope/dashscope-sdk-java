// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.task;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class AsyncTaskListParam extends HalfDuplexParamBase {
  String startTime;
  String endTime;
  String modelName;
  String apiKeyId;
  String region;
  String status;
  Integer pageNo;
  Integer pageSize;

  @Override
  public String getModel() {
    throw new UnsupportedOperationException("Unimplemented method 'getModel'");
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (startTime != null) {
      params.put("start_time", startTime);
    }
    if (endTime != null) {
      params.put("end_time", endTime);
    }
    if (modelName != null) {
      params.put("model_name", modelName);
    }
    if (apiKeyId != null) {
      params.put("api_key_id", apiKeyId);
    }
    if (region != null) {
      params.put("region", region);
    }
    if (status != null) {
      params.put("status", status);
    }
    if (pageNo != null) {
      params.put("page_no", pageNo);
    }
    if (pageSize != null) {
      params.put("page_size", pageSize);
    }
    params.putAll(this.parameters);
    return params;
  }

  @Override
  public JsonObject getHttpBody() {
    throw new UnsupportedOperationException("Unimplemented method 'getBatchData'");
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public Object getInput() {
    throw new UnsupportedOperationException("Unimplemented method 'getInput'");
  }

  @Override
  public void validate() throws InputRequiredException {}

  @Override
  public Object getResources() {
    return null;
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> res = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().toString();
      res.put(key, value);
    }
    return res;
  }
}
