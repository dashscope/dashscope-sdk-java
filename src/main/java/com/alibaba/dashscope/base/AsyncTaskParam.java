// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.base;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class AsyncTaskParam extends HalfDuplexParamBase {
  private String taskId;

  @Override
  public String getModel() {
    throw new UnsupportedOperationException("Unimplemented method 'getModel'");
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @Override
  public JsonObject getHttpBody() {
    return null;
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
}
