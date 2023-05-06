package com.dashscope.conversation.rpc;

import com.dashscope.common.Protocol;
import com.dashscope.common.ResultCallback;
import com.dashscope.conversation.*;
import com.dashscope.exception.ApiException;
import com.dashscope.utils.Constants;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

@Slf4j
public final class ConversationWebsocketRpc implements ConversationRpc {

  private final WebSocketClient client;

  private Class<? extends ConversationResultBase> resultType;

  private ResultCallback<ConversationResultBase> resultCallback = null;

  public ConversationWebsocketRpc() {
    try {
      client =
          new WebSocketClient(new URI(Constants.baseWebsocketApiUrl)) {
            @Override
            public void onOpen(ServerHandshake handShakeData) {
              assert (resultCallback != null);
              resultCallback.onOpen(
                  WebsocketConnectionStatus.builder()
                      .statusCode(handShakeData.getHttpStatus())
                      .message(handShakeData.getHttpStatusMessage())
                      .build());
            }

            @Override
            public void onMessage(String message) {
              try {
                assert (resultCallback != null && resultType != null);
                ConversationResultBase result = resultType.newInstance();
                result.loadFromMessage(Protocol.WEBSOCKET.getValue(), message);
                String eventType = result.getEventType();
                resultCallback.onEvent(eventType, result);
              } catch (Exception e) {
                resultCallback.onError(e instanceof ApiException ? e : new ApiException(e));
              }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
              assert (resultCallback != null);
              resultCallback.onClose(
                  WebsocketConnectionStatus.builder()
                      .statusCode(code)
                      .message(reason)
                      .remote(remote)
                      .build());
            }

            @Override
            public void onError(Exception ex) {
              assert (resultCallback != null);
              resultCallback.onError(new ApiException(ex));
            }

            @Override
            public void close(int code) {
              assert (resultCallback != null);
              resultCallback.doClose(WebsocketConnectionStatus.builder().statusCode(code).build());
              super.close(code);
            }
          };
    } catch (Exception e) {
      throw new ApiException(e);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    this.client.closeBlocking();
    super.finalize();
  }

  @Override
  public void call(ConversationParamBase param, ResultCallback<ConversationResultBase> callback) {
    try {
      this.resultCallback = callback;
      this.resultType = param.resultType();

      boolean connected = true;
      if (client.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
        assert (param.getApiKey() != null || Constants.apiKey != null);
        client.addHeader(
            "Authorization", param.getApiKey() == null ? Constants.apiKey : param.getApiKey());
        if (param.isSecurityCheck()) {
          client.addHeader("X-DashScope-DataInspection", "enable");
        }
        connected = client.connectBlocking();
      } else if (client.getReadyState() == ReadyState.CLOSED) {
        assert (param.getApiKey() != null || Constants.apiKey != null);
        client.addHeader(
            "Authorization", param.getApiKey() == null ? Constants.apiKey : param.getApiKey());
        if (param.isSecurityCheck()) {
          client.addHeader("X-DashScope-DataInspection", "enable");
        }
        connected = client.reconnectBlocking();
      }
      if (!connected) {;
        callback.onError(new ApiException(new WebsocketNotConnectedException()));
        return;
      }
      client.send(param.buildMessageBody(Protocol.WEBSOCKET.getValue()));
    } catch (Exception e) {
      this.resultCallback.onError(e instanceof ApiException ? e : new ApiException(e));
    }
  }
}
