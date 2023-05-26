// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

/** The websocket rpc, biz logic free. */
@Slf4j
public final class WebsocketRpc {

  private final WebSocketClient client;

  @Setter private ResultCallback<Response> responseCallback;

  @Setter private Function<String, Boolean> completeCallback;

  @Getter private final String url;

  @Getter private final Map<String, String> headers;

  /**
   * Construct the class.
   *
   * @param headers The headers to use for connection.
   * @param url The ws url.
   */
  public WebsocketRpc(String url, Map<String, String> headers) {
    this.url = url;
    this.headers = headers;
    try {
      client =
          new WebSocketClient(new URI(url)) {

            @Override
            public void onOpen(ServerHandshake handShakeData) {
              responseCallback.onOpen(
                  Status.builder()
                      .statusCode(handShakeData.getHttpStatus())
                      .message(handShakeData.getHttpStatusMessage())
                      .build());
            }

            @Override
            public void onMessage(String message) {
              responseCallback.onEvent(Response.builder().message(message).build());
              if (completeCallback.apply(message)) {
                responseCallback.onComplete();
              }
            }

            @Override
            public void onMessage(ByteBuffer message) {
              responseCallback.onEvent(Response.builder().binary(message).build());
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
              if ((reason == null ? "" : reason).contains("Unauthorized")) {
                responseCallback.onError(
                    new ApiException(
                        Status.builder()
                            .statusCode(HttpStatus.SC_UNAUTHORIZED)
                            .code(String.valueOf(code))
                            .message(reason)
                            .build()));
              } else {
                responseCallback.onClose(Status.builder().statusCode(code).message(reason).build());
              }
            }

            @Override
            public void onError(Exception ex) {
              responseCallback.onError(new ApiException(ex));
            }

            @Override
            public void close(int code) {
              responseCallback.doClose(Status.builder().statusCode(code).build());
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

  /**
   * Do call.
   *
   * @param responseCallback The response callback.
   * @param completeCallback A Function to identify whether the message has completed (whether to
   *     call onComplete).
   * @param requests The iterable input requests.
   */
  public void call(
      Flowable<Request> requests,
      ResultCallback<Response> responseCallback,
      Function<String, Boolean> completeCallback) {
    assert (responseCallback != null && completeCallback != null);
    this.responseCallback = responseCallback;
    this.completeCallback = completeCallback;
    try {
      boolean connected = true;
      if (client.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
          client.addHeader(header.getKey(), header.getValue());
        }
        connected = client.connectBlocking();
      } else if (client.getReadyState() == ReadyState.CLOSED) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
          client.addHeader(header.getKey(), header.getValue());
        }
        connected = client.reconnectBlocking();
      }
      if (!connected) {
        return;
      }

      requests.blockingForEach(
          request -> {
            if (request.getBinary() != null) {
              client.send(request.getBinary());
            } else {
              client.send(request.getMessage());
            }
          });

    } catch (Exception e) {
      this.responseCallback.onError(e instanceof ApiException ? e : new ApiException(e));
    }
  }

  /**
   * Do call.
   *
   * @param request The input request.
   * @param responseCallback The response callback.
   * @param completeCallback A Function to identify whether the message has completed (whether to
   *     call onComplete).
   */
  public void call(
      Request request,
      ResultCallback<Response> responseCallback,
      Function<String, Boolean> completeCallback) {
    call(Flowable.fromArray(request), responseCallback, completeCallback);
  }
}
