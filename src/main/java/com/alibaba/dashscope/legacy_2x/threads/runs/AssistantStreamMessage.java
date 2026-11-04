package com.alibaba.dashscope.legacy_2x.threads.runs;

import com.alibaba.dashscope.legacy_2x.common.FlattenResultBase;
import com.alibaba.dashscope.legacy_2x.threads.AssistantStreamEvents;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Represents streaming data and event pairs */
@Data
@EqualsAndHashCode(callSuper = true)
@Deprecated
public class AssistantStreamMessage extends FlattenResultBase {
  private AssistantStreamEvents event;
  private Object data;
}
