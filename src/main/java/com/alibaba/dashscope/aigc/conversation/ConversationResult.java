// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.conversation;

import static com.alibaba.dashscope.utils.ApiKeywords.TEXT;

import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.Response;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationResult extends Result {

  /** The chat message structure. */
  private ChatMessage message;

  /**
   * Load data from the server output.
   *
   * @param protocol The protocol, can be 'http' or 'websocket'.
   * @param response The server Response.
   * @throws ApiException if the input is invalid.
   */
  @Override
  public void fromResponse(Protocol protocol, Response response) throws ApiException {
    ConversationMessagePayload payload =
        JsonUtils.fromJson(response.getMessage(), ConversationMessagePayload.class);
    this.setUsage(payload.getUsage());
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
    this.setHeaders(response.getHeaders() == null ? Maps.newHashMap() : response.getHeaders());
  }
}
