// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.conversation;

import com.alibaba.dashscope.exception.ApiException;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Data
@SuperBuilder
public abstract class ConversationParam {

  /** The message id */
  private String msgId;

  /** The model to use. */
  private String model;

  /** The input prompt. */
  private String prompt;

  /** The chat history, if not provided, the api will use its internal */
  private List<ChatMessage> history = null;

  /** The apiKey. */
  private String apiKey;

  /** Get real-time response with streaming mode */
  private boolean stream = false;

  /** Open security check */
  private boolean securityCheck = true;

  public String getMsgId() {
    if (StringUtils.isBlank(msgId)) {
      msgId = UUID.randomUUID().toString();
    }
    return msgId;
  }

  /**
   * The url suffix to call the server side.
   *
   * @return The url suffix.
   */
  public abstract String url();

  /**
   * Build the message body by the protocol.
   *
   * @param protocol The protocol, can be 'http' or 'websocket'.
   * @return the message body
   * @throws ApiException if the input is invalid.
   */
  public abstract String buildMessageBody(String protocol) throws ApiException;

  /**
   * Get the related server output structure, which should be a subclass of `ConversationResult`.
   *
   * @return The server output Class.
   */
  public abstract Class<? extends ConversationResult> resultType();
}
