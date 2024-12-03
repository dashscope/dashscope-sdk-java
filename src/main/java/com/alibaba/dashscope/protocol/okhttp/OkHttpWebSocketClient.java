// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.DashScopeHeaders;
import com.alibaba.dashscope.protocol.FullDuplexClient;
import com.alibaba.dashscope.protocol.FullDuplexRequest;
import com.alibaba.dashscope.protocol.HalfDuplexClient;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.protocol.WebSocketResponse;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

@Slf4j
public class OkHttpWebSocketClient extends WebSocketListener
    implements HalfDuplexClient, FullDuplexClient {
  // we will try 3 times for connection.
  private static final int MAX_CONNECTION_TIMES = 3;
  private OkHttpClient client;
  private WebSocket webSocketClient;
  // indicate the websocket connection is established.
  private AtomicBoolean isOpen = new AtomicBoolean(false);
  // indicate the first response is received.
  private AtomicBoolean isFirstMessage = new AtomicBoolean(false);
  // used for get request response
  private FlowableEmitter<DashScopeResult> responseEmitter;
  // is the result is flatten format.
  private boolean isFlattenResult;
  private FlowableEmitter<DashScopeResult> connectionEmitter;

  public OkHttpWebSocketClient(OkHttpClient client) {
    this.client = client;
  }

  private Request buildConnectionRequest(
      String apiKey,
      boolean isSecurityCheck,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl)
      throws NoApiKeyException {
    // build the request builder.
    Builder bd = new Request.Builder();
    bd.headers(
        Headers.of(
            DashScopeHeaders.buildWebSocketHeaders(
                apiKey, isSecurityCheck, workspace, customHeaders)));
    String url = Constants.baseWebsocketApiUrl;
    if (baseWebSocketUrl != null) {
      url = baseWebSocketUrl;
    }
    Request request = bd.url(url).build();
    return request;
  }

  public boolean close(int code, String reason) {
    /**
     * close websocket connection see.
     * https://square.github.io/okhttp/3.x/okhttp/okhttp3/WebSocket.html
     */
    if (webSocketClient != null) {
      return webSocketClient.close(code, reason);
    } else {
      return true;
    }
  }

  public void cancel() {
    if (webSocketClient != null) {
      webSocketClient.cancel();
    }
  }

  private void establishWebSocketClient(
      String apiKey,
      boolean isSecurityCheck,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl) {
    int reconnectionTimes = 0;
    String errorMessage = "";
    while (reconnectionTimes < MAX_CONNECTION_TIMES) {
      try {
        Flowable<DashScopeResult> flowable =
            Flowable.<DashScopeResult>create(
                emitter -> {
                  this.connectionEmitter = emitter;
                  try {
                    client = OkHttpClientFactory.getOkHttpClient();
                    webSocketClient =
                        client.newWebSocket(
                            buildConnectionRequest(
                                apiKey,
                                isSecurityCheck,
                                workspace,
                                customHeaders,
                                baseWebSocketUrl),
                            this);
                  } catch (Throwable ex) {
                    this.connectionEmitter.onError(ex);
                  }
                },
                BackpressureStrategy.BUFFER);
        // wait for connection establish
        flowable.blockingSubscribe();
        return;
      } catch (Throwable ex) {
        reconnectionTimes += 1;
        errorMessage = ex.getMessage();
        log.error(errorMessage);
        if (errorMessage.contains("401 Unauthorized")) {
          break;
        } else if (errorMessage.contains(Constants.NO_API_KEY_ERROR)) {
          throw ex;
        }
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {;
        }
      }
    }
    throw new ApiException(
        Status.builder()
            .code("ConnectionError")
            .message(errorMessage)
            .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
            .build());
  }

  @Override
  public void onClosed(WebSocket webSocket, int code, String reason) {
    // Invoked when both peers have indicated that no more messages will be
    // transmitted and the connection has been successfully released. No further
    // calls to this
    // listener will be made.
    log.debug(String.format("WebSocket %s closed: %d, %s", webSocket.toString(), code, reason));
    isOpen.set(false);
  }

  @Override
  public void onClosing(WebSocket webSocket, int code, String reason) {
    // Invoked when the remote peer has indicated that no more incoming messages
    // will be
    // transmitted.
    // 服务端异常也会close code 1001需要处理
    // RFC 6455
    // Endpoints MAY use the following pre-defined status codes when sending a Close
    // frame.
    // 1000 indicates a normal closure, meaning that the purpose for which the
    // connection was established has been fulfilled.
    // 1001 indicates that an endpoint is "going away", such as a server going down
    // or a browser having navigated away from a page.
    // 1002 indicates that an endpoint is terminating the connection due to a
    // protocol error.
    // 1003 indicates that an endpoint is terminating the connection because it has
    // received a type of data it cannot accept (e.g., an
    // endpoint that understands only text data MAY send this if it receives a
    // binary message)
    webSocket.close(code, null);
    log.debug(String.format("Websocket is closing, code: %s, reasion: %s", code, reason));
    if (responseEmitter != null && !responseEmitter.isCancelled()) {
      responseEmitter.onComplete();
    } else { // close on idle, such as server close the connection.
      ;
    }
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    // Invoked when a web socket has been closed due to an error reading from or
    // writing to the network.
    // Both outgoing and incoming messages may have been lost. No further calls to
    // this listener will be made.

    String responseBody = "";
    // Get response body if there is.
    if (response != null) {
      try {
        responseBody = response.body().string();
      } catch (IOException ex) {
        log.error(ex.getMessage());
      }
    }
    String failureMessage =
        String.format(
            "Websocket failure %s, cause: %s, body: %s",
            t.getMessage(), t.getCause(), responseBody);
    log.error(failureMessage);
    isOpen.set(false);
    if (connectionEmitter != null && !connectionEmitter.isCancelled()) {
      connectionEmitter.onError(new Exception(failureMessage, t));
    } else if (responseEmitter != null && !responseEmitter.isCancelled()) {
      // error on request
      responseEmitter.onError(new Exception(failureMessage, t));
    } else {
      log.error(failureMessage);
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    log.debug(text);
    // Invoked when a text (type 0x1) message has been received.
    if (!isFirstMessage.get()) {
      log.debug("Receive first package.");
      isFirstMessage.set(true);
    }
    try {
      // Check different message.
      WebSocketResponse response = JsonUtils.fromJson(text, WebSocketResponse.class);
      switch (response.header.event) {
        case TASK_STARTED:
          // if has payload, call onNext.
          if (response.payload.output != null || response.payload.usage != null) {
            responseEmitter.onNext(
                new DashScopeResult()
                    .fromResponse(
                        Protocol.WEBSOCKET,
                        NetworkResponse.builder().message(text).build(),
                        isFlattenResult));
          }
          break;
        case TASK_FAILED:
          log.error(String.format("Receive task_failed message: %s", text));
          Status st =
              Status.builder()
                  .code(response.header.code)
                  .message(response.header.message)
                  .requestId(response.header.taskId)
                  .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
                  .isJson(true)
                  .build();
          // throw new ApiException(st);
          if (!responseEmitter.isCancelled()) {
            responseEmitter.onError(new ApiException(st));
          } else {
            log.error(String.format("Something wrong, receive task failed message: %s", text));
          }
        case TASK_FINISHED:
          // check the payload and usage is null.
          if (response.payload.output != null || response.payload.usage != null) {
            responseEmitter.onNext(
                new DashScopeResult()
                    .fromResponse(
                        Protocol.WEBSOCKET,
                        NetworkResponse.builder().message(text).build(),
                        isFlattenResult));
          }
          responseEmitter.onComplete();
          break;
        case RESULT_GENERATED:
          // get payload and usage.
          responseEmitter.onNext(
              new DashScopeResult()
                  .fromResponse(
                      Protocol.WEBSOCKET,
                      NetworkResponse.builder().message(text).build(),
                      isFlattenResult));
          break;
        default:
          // throw new ApiException(Status.builder().code("")
          // .message(String.format("Receive unknown message: %s", text))
          // .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE).build());
          responseEmitter.onError(
              new ApiException(
                  Status.builder()
                      .code("UnknownMessage")
                      .message(String.format("Receive unknown message: %s", text))
                      .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
                      .build()));
      }
    } catch (Throwable ex) {
      responseEmitter.onError(
          new ApiException(
              Status.builder()
                  .code("MessageFormatError")
                  .message(String.format("Receive message: %s, json deserialize exception", text))
                  .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
                  .build()));
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteString bytes) {
    // Invoked when a binary (type 0x2) message has been received.
    if (!isFirstMessage.get()) {
      log.debug("Receive first binary package.");
      isFirstMessage.set(true);
    }
    responseEmitter.onNext(
        new DashScopeResult()
            .fromResponse(
                Protocol.WEBSOCKET,
                NetworkResponse.builder().binary(bytes.asByteBuffer()).build(),
                isFlattenResult));
  }

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    // the connection has been accepted by the remote peer and may begin
    // transmitting messages
    // Invoked when a web socket has been accepted by the remote peer and may begin
    // transmitting
    // messages..
    isOpen.set(true);
    if (connectionEmitter != null && !connectionEmitter.isCancelled()) {
      connectionEmitter.onComplete();
    }
  }

  private void sendTextWithRetry(
      String apiKey,
      boolean isSecurityCheck,
      String message,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl) {
    // simple retry with fixed delay, no strategy
    if (!isOpen.get()) {
      establishWebSocketClient(apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
    }
    int maxRetries = 3;
    int retryCount = 0;
    while (retryCount < maxRetries) {
      log.debug("Sending message: " + message);
      Boolean isOk = webSocketClient.send(message);
      if (isOk) {
        break;
      } else {
        establishWebSocketClient(
            apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
        log.warn(
            String.format(
                "Send request failed, the connection may closed, will reconnect and send again"));
      }
      Observable.timer(5000, TimeUnit.MILLISECONDS).blockingSingle();
      ++retryCount;
    }
  }

  private void sendBinaryWithRetry(
      String apiKey,
      boolean isSecurityCheck,
      ByteString message,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl) {
    if (!isOpen.get()) {
      establishWebSocketClient(apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
    }
    int maxRetries = 3;
    int retryCount = 0;
    while (retryCount < maxRetries) {
      Boolean isOk = webSocketClient.send(message);
      if (isOk) {
        break;
      } else {
        establishWebSocketClient(
            apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
        log.warn(
            String.format(
                "Send request failed, the connection may closed, will reconnect and send again"));
      }
      Observable.timer(5000, TimeUnit.MILLISECONDS).blockingSingle();
      ++retryCount;
    }
  }

  private void sendBatchRequest(HalfDuplexRequest req) {
    if (req.getWebsocketBinaryData() != null) {
      // send start-task.
      sendTextWithRetry(
          req.getApiKey(),
          req.isSecurityCheck(),
          JsonUtils.toJson(req.getStartTaskMessage()),
          req.getWorkspace(),
          req.getHeaders(),
          req.getBaseWebSocketUrl());
      // send binary data.
      sendBinaryWithRetry(
          req.getApiKey(),
          req.isSecurityCheck(),
          ByteString.of(req.getWebsocketBinaryData()),
          req.getWorkspace(),
          req.getHeaders(),
          req.getBaseWebSocketUrl());
    } else {
      // data and start-task in same package.
      sendTextWithRetry(
          req.getApiKey(),
          req.isSecurityCheck(),
          JsonUtils.toJson(req.getStartTaskMessage()),
          req.getWorkspace(),
          req.getHeaders(),
          req.getBaseWebSocketUrl());
    }
  }

  @Override
  public DashScopeResult send(HalfDuplexRequest req) {
    // send the request out.
    if (req.getStreamingMode() == StreamingMode.NONE
        || req.getStreamingMode() == StreamingMode.IN) {
      Flowable<DashScopeResult> flowable =
          Flowable.<DashScopeResult>create(
              emitter -> {
                this.responseEmitter = emitter;
                this.isFlattenResult = req.getIsFlatten();
              },
              BackpressureStrategy.BUFFER);
      flowable.subscribe().dispose();
      sendBatchRequest(req);
      return flowable.blockingSingle();
    } else {
      throw new ApiException(
          Status.builder()
              .code("Invalid call")
              .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
              .message("Please use streamOut interface of websocket.")
              .build());
    }
  }

  @Override
  public void send(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback) {
    if (req.getStreamingMode() == StreamingMode.NONE
        || req.getStreamingMode() == StreamingMode.IN) {
      Flowable<DashScopeResult> flowable =
          Flowable.<DashScopeResult>create(
              emitter -> {
                this.responseEmitter = emitter;
                this.isFlattenResult = req.getIsFlatten();
              },
              BackpressureStrategy.BUFFER);
      flowable.subscribe().dispose();
      sendBatchRequest(req);
      flowable.subscribe(
          msg -> {
            callback.onEvent(msg);
          },
          err -> {
            callback.onError(new ApiException(err));
          },
          new Action() {
            @Override
            public void run() throws Exception {
              callback.onComplete();
            }
          });
    } else {
      throw new ApiException(
          Status.builder()
              .code("Invalid call")
              .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
              .message("Please use streamOut interface of websocket.")
              .build());
    }
  }

  @Override
  public Flowable<DashScopeResult> streamOut(HalfDuplexRequest req) {
    // Set receive
    Flowable<DashScopeResult> flowable =
        Flowable.<DashScopeResult>create(
            emitter -> {
              this.responseEmitter = emitter;
              this.isFlattenResult = req.getIsFlatten();
            },
            BackpressureStrategy.BUFFER);
    flowable.subscribe().dispose();
    // send the request out.
    sendBatchRequest(req);
    return flowable;
  }

  @Override
  public void streamOut(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback) {
    Flowable<DashScopeResult> flowable = streamOut(req);
    flowable.subscribe(
        msg -> {
          callback.onEvent(msg);
        },
        err -> {
          callback.onError(new ApiException(err));
        },
        new Action() {
          @Override
          public void run() throws Exception {
            callback.onComplete();
          }
        });
  }

  private CompletableFuture<Void> sendStreamRequest(FullDuplexRequest req) {
    CompletableFuture<Void> future =
        CompletableFuture.runAsync(
            () -> {
              try {
                isFirstMessage.set(false);

                JsonObject startMessage = req.getStartTaskMessage();
                String taskId =
                    startMessage.get("header").getAsJsonObject().get("task_id").getAsString();
                // send start message out.
                sendTextWithRetry(
                    req.getApiKey(),
                    req.isSecurityCheck(),
                    JsonUtils.toJson(startMessage),
                    req.getWorkspace(),
                    req.getHeaders(),
                    req.getBaseWebSocketUrl());

                Flowable<Object> streamingData = req.getStreamingData();
                streamingData.subscribe(
                    data -> {
                      try {
                        if (data instanceof String) {
                          JsonObject continueData = req.getContinueMessage((String) data, taskId);
                          sendTextWithRetry(
                              req.getApiKey(),
                              req.isSecurityCheck(),
                              JsonUtils.toJson(continueData),
                              req.getWorkspace(),
                              req.getHeaders(),
                              req.getBaseWebSocketUrl());
                        } else if (data instanceof byte[]) {
                          sendBinaryWithRetry(
                              req.getApiKey(),
                              req.isSecurityCheck(),
                              ByteString.of((byte[]) data),
                              req.getWorkspace(),
                              req.getHeaders(),
                              req.getBaseWebSocketUrl());
                        } else if (data instanceof ByteBuffer) {
                          sendBinaryWithRetry(
                              req.getApiKey(),
                              req.isSecurityCheck(),
                              ByteString.of((ByteBuffer) data),
                              req.getWorkspace(),
                              req.getHeaders(),
                              req.getBaseWebSocketUrl());
                        } else {
                          JsonObject continueData = req.getContinueMessage(data, taskId);
                          sendTextWithRetry(
                              req.getApiKey(),
                              req.isSecurityCheck(),
                              JsonUtils.toJson(continueData),
                              req.getWorkspace(),
                              req.getHeaders(),
                              req.getBaseWebSocketUrl());
                        }
                      } catch (Throwable ex) {
                        log.error(String.format("sendStreamData exception: %s", ex.getMessage()));
                        responseEmitter.onError(ex);
                      }
                    },
                    err -> {
                      log.error(String.format("Get stream data error!"));
                      responseEmitter.onError(err);
                    },
                    new Action() {
                      @Override
                      public void run() throws Exception {
                        log.debug(String.format("Stream data send completed!"));
                        sendTextWithRetry(
                            req.getApiKey(),
                            req.isSecurityCheck(),
                            JsonUtils.toJson(req.getFinishedTaskMessage(taskId)),
                            req.getWorkspace(),
                            req.getHeaders(),
                            req.getBaseWebSocketUrl());
                      }
                    });
              } catch (Throwable ex) {
                log.error(String.format("sendStreamData exception: %s", ex.getMessage()));
                responseEmitter.onError(ex);
              }
            });
    return future;
  }

  private void joinSendFuture(CompletableFuture<Void> future) {
    try {
      if (future.isDone()) {
        future.join();
      } else {
        future.cancel(true);
        future.join();
      }
    } catch (CancellationException | CompletionException ex) {
      log.error("Sending streaming data exception", ex.getMessage());
    }
  }

  @Override
  public DashScopeResult streamIn(FullDuplexRequest req) {
    Flowable<DashScopeResult> flowable =
        Flowable.<DashScopeResult>create(
            emitter -> {
              this.responseEmitter = emitter;
              this.isFlattenResult = req.getIsFlatten();
            },
            BackpressureStrategy.BUFFER);
    flowable.subscribe().dispose();
    CompletableFuture<Void> future = sendStreamRequest(req);
    DashScopeResult result =
        flowable
            .doOnError(
                err -> {
                  joinSendFuture(future);
                })
            .doOnComplete(
                new Action() {
                  @Override
                  public void run() throws Exception {
                    joinSendFuture(future);
                  }
                })
            .blockingFirst();
    return result;
  }

  @Override
  public void streamIn(FullDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException {
    DashScopeResult res = streamIn(req);
    callback.onEvent(res);
    callback.onComplete();
  }

  @Override
  public Flowable<DashScopeResult> duplex(FullDuplexRequest req)
      throws NoApiKeyException, ApiException {
    Flowable<DashScopeResult> flowable =
        Flowable.<DashScopeResult>create(
            emitter -> {
              this.responseEmitter = emitter;
              this.isFlattenResult = req.getIsFlatten();
            },
            BackpressureStrategy.BUFFER);
    flowable.subscribe().dispose();
    CompletableFuture<Void> future = sendStreamRequest(req);

    return flowable
        .doOnError(
            err -> {
              joinSendFuture(future);
            })
        .doOnComplete(
            new Action() {
              @Override
              public void run() throws Exception {
                joinSendFuture(future);
              }
            });
  }

  @Override
  public void duplex(FullDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException {
    Flowable<DashScopeResult> flowable = duplex(req);
    flowable.subscribe(
        msg -> {
          callback.onEvent(msg);
        },
        err -> {
          callback.onError(new ApiException(err));
        },
        new Action() {
          @Override
          public void run() throws Exception {
            callback.onComplete();
          }
        });
  }
}
