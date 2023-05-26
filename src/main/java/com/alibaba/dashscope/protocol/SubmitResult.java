package com.alibaba.dashscope.protocol;

import static com.alibaba.dashscope.utils.ApiKeywords.OUTPUT;
import static com.alibaba.dashscope.utils.ApiKeywords.TASKID;

import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Result;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class SubmitResult extends Result {

  private String taskId;

  @Override
  public void fromResponse(Protocol protocol, Response response) throws ApiException {
    assert (protocol == Protocol.HTTP);
    JsonObject jsonObject = JsonUtils.parse(response.getMessage());
    JsonObject output = jsonObject.getAsJsonObject(OUTPUT);
    taskId = output.get(TASKID) == null ? null : output.get(TASKID).getAsString();
  }
}
