package com.alibaba.dashscope.conversation;

import com.alibaba.dashscope.common.Status;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ConversationMessageStatus extends Status {

  /** The error code from the server side. */
  private String code;

  /** The message id of the request. */
  private String msgId;

  /** The usage information. */
  private JsonObject usage;
}
