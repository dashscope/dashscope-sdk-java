package com.dashscope.conversation;

import com.alibaba.fastjson.JSONObject;
import com.dashscope.common.Status;
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
  private JSONObject usage;
}
