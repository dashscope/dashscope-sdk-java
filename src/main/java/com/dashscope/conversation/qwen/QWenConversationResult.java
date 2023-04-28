package com.dashscope.conversation.qwen;

import static com.dashscope.utils.ApiKeywords.TEXT;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dashscope.common.ErrorType;
import com.dashscope.common.Protocol;
import com.dashscope.common.Role;
import com.dashscope.conversation.*;
import com.dashscope.exception.ApiException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class QWenConversationResult extends ConversationResultBase {

  /** The data usage information. */
  private JSONObject usage;

  @Override
  public void loadFromMessage(String protocol, String message) {
    if (Protocol.HTTP.getValue().equals(protocol)) {
      ConversationMessagePayload payload =
          JSON.parseObject(message, ConversationMessagePayload.class);
      this.setMessage(
          payload.getOutput() != null
              ? ChatMessage.builder()
                  .role(Role.BOT.getValue())
                  .payload(payload.getOutput().getString(TEXT))
                  .build()
              : null);
      this.setUsage(payload.getUsage());
      this.setMsgId(payload.getRequestId());
    } else if (Protocol.WEBSOCKET.getValue().equals(protocol)) {
      ConversationMessage res = JSON.parseObject(message, ConversationMessage.class);
      ConversationMessageHeader header = res.getHeader();
      ConversationMessagePayload payload = res.getPayload();
      JSONObject usage = payload.getUsage();
      if (EventType.TASK_FAILED.getValue().equals(header.getEvent())) {
        throw new ApiException(
            ConversationMessageStatus.builder()
                .message(header.getErrorMessage())
                .statusCode(HttpStatus.SC_OK)
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
                  .payload(payload.getOutput().getString(TEXT))
                  .build()
              : null);
      this.setUsage(usage);
    } else {
      throw new ApiException(
          ConversationMessageStatus.builder()
              .code(ErrorType.PROTOCOL_UNSUPPORTED.getValue())
              .message(protocol)
              .build());
    }
  }
}
