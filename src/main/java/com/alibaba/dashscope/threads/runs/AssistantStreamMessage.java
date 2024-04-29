package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.threads.AssistantStreamEvents;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Represents streaming data and event pairs */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssistantStreamMessage extends FlattenResultBase {
  private AssistantStreamEvents event;
  private Object data;
}
