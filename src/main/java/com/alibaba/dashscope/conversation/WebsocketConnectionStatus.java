package com.alibaba.dashscope.conversation;

import com.alibaba.dashscope.common.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public final class WebsocketConnectionStatus extends Status {

  private Boolean remote;
}
