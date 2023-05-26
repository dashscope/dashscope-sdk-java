package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.Param;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Result;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class SubmitParam extends Param {

  private String taskId;

  @Override
  public String url() {
    return "/tasks/" + taskId;
  }

  @Override
  public Request toRequest(Protocol protocol) {
    return null;
  }

  @Override
  public Class<? extends Result> resultType() {
    return null;
  }
}
