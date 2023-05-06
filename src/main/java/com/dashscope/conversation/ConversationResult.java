package com.dashscope.conversation;

import com.dashscope.exception.ApiException;
import lombok.Data;

@Data
public abstract class ConversationResult {

  /** The msg id. */
  private String msgId;

  /** The message type. Please check `EventType` for details. */
  private String eventType;

  /** The chat message structure. */
  private ChatMessage message;

  /**
   * Load data from the server output.
   *
   * @param protocol The protocol, can be 'http' or 'websocket'.
   * @param message The server message.
   * @throws ApiException if the input is invalid.
   */
  public abstract void loadFromMessage(String protocol, String message) throws ApiException;
}
