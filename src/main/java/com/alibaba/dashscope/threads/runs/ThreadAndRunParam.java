package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.threads.ThreadParam;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ThreadAndRunParam extends RunParam {
  private ThreadParam thread;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = super.getHttpBody();
    if (thread != null) {
      requestObject.add("thread", thread.getHttpBody());
    }
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("The assistantId must be set");
    }
    if (responseFormat != null) {
      if (responseFormat instanceof String) {
        if (((String) responseFormat).equals("json_object")) {
          return;
        }
      }
    }
    throw new InputRequiredException("The response format only support json_object");
  }
}
