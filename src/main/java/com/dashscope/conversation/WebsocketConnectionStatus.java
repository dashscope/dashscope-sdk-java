package com.dashscope.conversation;

import com.dashscope.common.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public final class WebsocketConnectionStatus extends Status {

  private Boolean remote;
}
