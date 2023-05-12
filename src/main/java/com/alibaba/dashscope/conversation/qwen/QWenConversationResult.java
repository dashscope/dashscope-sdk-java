// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.conversation.qwen;

import static com.alibaba.dashscope.utils.ApiKeywords.TEXT;

import com.alibaba.dashscope.common.ErrorType;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.conversation.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class QWenConversationResult extends ConversationResult {

  /** The data usage information. */
  private JsonObject usage;

  @Override
  public void loadFromMessage(String protocol, String message) {
    if (Protocol.HTTP.getValue().equals(protocol)) {
      ConversationMessagePayload payload =
          JsonUtils.fromJson(message, ConversationMessagePayload.class);
      this.setMessage(
          payload.getOutput() != null
              ? ChatMessage.builder()
                  .role(Role.BOT.getValue())
                  .payload(
                      payload.getOutput().get(TEXT) == null
                          ? null
                          : payload.getOutput().get(TEXT).getAsString())
                  .build()
              : null);
      this.setUsage(payload.getUsage());
      this.setMsgId(payload.getRequestId());
    } else if (Protocol.WEBSOCKET.getValue().equals(protocol)) {
      ConversationMessage res = JsonUtils.fromJson(message, ConversationMessage.class);
      ConversationMessageHeader header = res.getHeader();
      ConversationMessagePayload payload = res.getPayload();
      JsonObject usage = payload.getUsage();
      if (EventType.TASK_FAILED.getValue().equals(header.getEvent())) {
        throw new ApiException(
            ConversationMessageStatus.builder()
                .message(header.getErrorMessage())
                .statusCode(44)
                .code(header.getErrorName())
                .msgId(header.getTaskId())
                .usage(usage)
                .build());
      }
      this.setEventType(header.getEvent());
      this.setMsgId(header.getTaskId());
      this.setMessage(
          payload.getOutput() != null
              ? ChatMessage.builder()
                  .role(Role.BOT.getValue())
                  .payload(
                      payload.getOutput().get(TEXT) == null
                          ? null
                          : payload.getOutput().get(TEXT).getAsString())
                  .build()
              : null);
      this.setUsage(usage);
    } else {
      throw new ApiException(
          ConversationMessageStatus.builder()
              .code(ErrorType.PROTOCOL_UNSUPPORTED.getValue())
              .message(protocol)
              .statusCode(HttpStatus.SC_BAD_REQUEST)
              .build());
    }
  }
}
