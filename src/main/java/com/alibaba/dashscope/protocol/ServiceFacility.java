package com.alibaba.dashscope.protocol;

import static com.alibaba.dashscope.utils.ApiKeywords.*;
import static java.lang.Thread.sleep;

import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.pool.WebsocketPool;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.Builder;
import lombok.Data;
import lombok.var;
import org.apache.http.HttpStatus;

/** The rpc factory to use both http rpc and ws rpc. */
public class ServiceFacility {

  /**
   * Call the network to do async task submitting and queries. Only support http.
   *
   * @param extraHeaders Extra headers to pass, basic headers will be built inside this function.
   * @param param The input param to submit the task.
   * @param method The http method.
   * @param receiveTimeout The reception timeout of a single message.
   * @return The final result after waiting.
   */
  public static Result callAndWait(
      Map<String, String> extraHeaders, Param param, HttpMethod method, long receiveTimeout) {
    var headers = prepareHeaders(Protocol.HTTP, param);
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }
    SubmitResult result =
        (SubmitResult)
            call(Protocol.HTTP, extraHeaders, param, method, receiveTimeout, SubmitResult.class);
    SubmitParam submitParam =
        SubmitParam.builder().taskId(result.getTaskId()).apiKey(param.getApiKey()).build();
    return wait(submitParam, param.resultType(), receiveTimeout);
  }

  /**
   * Wait for an async task to finish, check every 5 seconds. Only support http.
   *
   * @param submitParam The `SubmitParam` used to query the task.
   * @param clazz The result class.
   * @param receiveTimeout The reception timeout.
   * @return The task result.
   */
  public static Result wait(
      SubmitParam submitParam, Class<? extends Result> clazz, long receiveTimeout) {
    while (true) {
      try {
        submitParam.setMode(StreamingMode.NONE);
        var headers = prepareHeaders(Protocol.HTTP, submitParam);
        String url = prepareUrl(Protocol.HTTP, submitParam);
        Response response =
            streamingNone(
                Protocol.HTTP,
                url,
                headers,
                prepareRequest(Protocol.HTTP, submitParam, WebSocketEventType.RUN_TASK),
                HttpMethod.GET,
                receiveTimeout);
        JsonObject jsonObject = JsonUtils.parse(response.getMessage());
        JsonObject output = jsonObject.getAsJsonObject(OUTPUT);
        String taskStatus =
            output.get(TASK_STATUS) == null ? null : output.get(TASK_STATUS).getAsString();
        if (TaskStatus.FAILED.getValue().equals(taskStatus)
            || TaskStatus.CANCELED.getValue().equals(taskStatus)
            || TaskStatus.SUCCEEDED.getValue().equals(taskStatus)
            || TaskStatus.UNKNOWN.getValue().equals(taskStatus)) {
          return prepareResult(Protocol.HTTP, response, clazz);
        }
      } catch (ApiException e) {
        if (e.getStatus().getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE
            && e.getStatus().getStatusCode() != HttpStatus.SC_GATEWAY_TIMEOUT) {
          throw e;
        }
      } finally {
        try {
          sleep(5000);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  /**
   * Call the network to do a stream-in, stream-out request. Only support websocket. This function
   * will automatically construct the Request and the Result, and build url, headers, body, etc. The
   * websocket command start-task/continue-task/finish-task will also be built here, so user need
   * only to pass the payload and some necessary information into the params. If the payload is
   * binaries, the first message may be binary+parameters, or parameters only.
   *
   * <p>You can use this function to send binary(binaries).
   *
   * @param extraHeaders Extra headers to pass, basic headers will be built inside this function.
   * @param params The input param containing the necessary payload information.
   * @param receiveTimeout The reception timeout of a single message.
   * @return The results in a Flowable
   */
  public static Flowable<Result> streamCall(
      Map<String, String> extraHeaders, Flowable<Param> params, long receiveTimeout) {
    var wsParam = prepareWsParams(params);
    Flowable<Request> requests = wsParam.getRequests();
    Param param = wsParam.getFirstParam();
    var headers = prepareHeaders(Protocol.WEBSOCKET, param);
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }
    return streamingInAndDuplex(Constants.baseWebsocketApiUrl, headers, requests, receiveTimeout)
        .map(response -> prepareResult(Protocol.WEBSOCKET, param, response));
  }

  static class DuplexCallback extends RpcResponseCallback<Result> {

    public DuplexCallback(
        ResultCallback<Result> callback,
        Function<Response, Result> converter,
        WebsocketRpc websocketRpc) {
      super(callback, converter, websocketRpc);
    }

    @Override
    public void onEvent(Response message) {
      if (isStartEvent(message.getMessage())) {
        return;
      }
      super.onEvent(message);
    }
  }

  /**
   * Call the network to do a single-in, stream-out request via a callback. Only support websocket.
   * This function will automatically construct the Request and the Result, and build url, headers,
   * body, etc. The websocket command start-task/continue-task/finish-task will also be built here,
   * so user need only to pass the payload and some necessary information into the params. The
   * callback is supposed to receive the Result and the error messages.
   *
   * <p>You can use this function to send binary(binaries).
   *
   * @param extraHeaders Extra headers to pass, basic headers will be built inside this function.
   * @param params The input param containing the necessary payload information.
   * @param resultCallback The callback to receive messages.
   */
  public static void streamCall(
      Map<String, String> extraHeaders,
      Flowable<Param> params,
      ResultCallback<Result> resultCallback) {
    var wsParam = prepareWsParams(params);
    Flowable<Request> requests = wsParam.getRequests();
    Param param = wsParam.getFirstParam();
    var headers = prepareHeaders(Protocol.WEBSOCKET, param);
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }

    streamingInAndDuplexWithCallback(
        Constants.baseWebsocketApiUrl,
        headers,
        requests,
        new DuplexCallback(
            resultCallback, input -> prepareResult(Protocol.WEBSOCKET, param, input), null));
  }

  /**
   * Call the network to do a single-in, stream-out request. This function will automatically
   * construct the Request and the Result, and build url, headers, body, etc. The websocket command
   * start-task will also be built here, so user need only to pass the payload and some necessary
   * information into the Param.
   *
   * <p>This function does not support binary.
   *
   * @param protocol The protocol.
   * @param extraHeaders Extra headers to pass, basic headers will be built inside this function.
   * @param param The input param containing the necessary payload information.
   * @param receiveTimeout The reception timeout.
   * @return The iterable result.
   * @throws ApiException when error occurs.
   */
  public static Flowable<Result> streamCall(
      Protocol protocol, Map<String, String> extraHeaders, Param param, long receiveTimeout) {
    param.setMode(StreamingMode.OUT);
    var headers = prepareHeaders(protocol, param);
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }
    String url = prepareUrl(protocol, param);
    return streamingOut(
            protocol,
            url,
            headers,
            prepareRequest(protocol, param, WebSocketEventType.RUN_TASK),
            HttpMethod.POST,
            StreamingMode.OUT,
            receiveTimeout)
        .map(response -> prepareResult(protocol, param, response));
  }

  /**
   * Call the network to do a single-in, single-out request. This function will automatically
   * construct the Request and the Result, and build url, headers, body, etc. The websocket command
   * start-task will also be built here, so user need only to pass the payload and some necessary
   * information into the Param.
   *
   * <p>This function does not support binary.
   *
   * @param protocol The protocol.
   * @param extraHeaders Extra headers to pass, basic headers will be built inside this function.
   * @param param The input param containing the necessary payload information.
   * @param method The http method.
   * @param receiveTimeout The reception timeout.
   * @return The result.
   * @throws ApiException when error occurs.
   */
  public static Result call(
      Protocol protocol,
      Map<String, String> extraHeaders,
      Param param,
      HttpMethod method,
      long receiveTimeout)
      throws ApiException {
    return call(protocol, extraHeaders, param, method, receiveTimeout, param.resultType());
  }

  private static Result call(
      Protocol protocol,
      Map<String, String> extraHeaders,
      Param param,
      HttpMethod method,
      long receiveTimeout,
      Class<? extends Result> clazz)
      throws ApiException {
    param.setMode(StreamingMode.NONE);
    var headers = prepareHeaders(protocol, param);
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }
    try {
      String url = prepareUrl(protocol, param);
      Response response =
          streamingNone(
              protocol,
              url,
              headers,
              prepareRequest(protocol, param, WebSocketEventType.RUN_TASK),
              method,
              receiveTimeout);
      return prepareResult(protocol, response, clazz);
    } catch (Exception e) {
      throw e instanceof ApiException ? (ApiException) e : new ApiException(e);
    }
  }

  /**
   * Prepare the connecting url.
   *
   * @param protocol The protocol.
   * @param param The input param.
   * @return The constructed url.
   * @throws ApiException when the protocol is not supported.
   */
  public static String prepareUrl(Protocol protocol, Param param) throws ApiException {
    switch (protocol) {
      case HTTP:
        String baseUrl = Constants.baseHttpApiUrl;
        if (baseUrl.endsWith("/")) {
          baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + param.url();
      case WEBSOCKET:
        return Constants.baseWebsocketApiUrl;
      default:
        throw new ApiException(
            Status.builder()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .code(ErrorType.PROTOCOL_UNSUPPORTED.getValue())
                .message("Invalid protocol: " + protocol)
                .build());
    }
  }

  /**
   * Prepare common headers.
   *
   * @param protocol The protocol.
   * @param param The input param.
   * @return Headers.
   */
  public static Map<String, String> prepareHeaders(Protocol protocol, Param param) {
    Map<String, String> headers = Maps.newHashMap();
    headers.put("Authorization", param.getApiKey());
    if (param.isSecurityCheck()) {
      headers.put("X-DashScope-DataInspection", "enable");
    }
    if (protocol == Protocol.HTTP) {
      headers.put("Authorization", "Bearer " + param.getApiKey());
      headers.put("Content-Type", "application/json");
      if (param.getMode() == StreamingMode.OUT) {
        headers.put("Accept", "text/event-stream");
        headers.put("X-Accel-Buffering", "no");
        headers.put("X-DashScope-SSE", "enable");
      } else if (param.getMode() == StreamingMode.NONE) {
        headers.put("Accept", "application/json");
      }
    }
    return headers;
  }

  /**
   * Build the message body by the protocol.
   *
   * @param protocol The protocol, can be 'http' or 'websocket'.
   * @param param The input param
   * @param eventType The WebSocketEventType to built into the Request
   * @return The message body
   * @throws ApiException if the input is invalid.
   */
  public static Request prepareRequest(
      Protocol protocol, Param param, WebSocketEventType eventType) {
    Request request = param.toRequest(protocol);
    if (protocol == Protocol.WEBSOCKET) {
      if (eventType == WebSocketEventType.RUN_TASK
          || eventType == WebSocketEventType.FINISH_TASK
          || request.getMessage() != null) {
        WsHeader header =
            WsHeader.buildInputHeader(param.getRequestId(), param.getMode(), eventType);
        Map<String, Object> map = Maps.newHashMap();
        map.put(HEADER, header);
        map.put(
            PAYLOAD,
            request.getMessage() == null
                ? new JsonObject()
                : JsonUtils.parse(request.getMessage()));
        request.setMessage(JsonUtils.toJson(map));
        if (eventType == WebSocketEventType.FINISH_TASK) {
          request.setBinary(null);
        }
      }
    }
    return request;
  }

  @Data
  @Builder
  static class WsParam {
    private Flowable<Request> requests;
    private Param firstParam;
  }

  /**
   * Add start-task and finish-task to the input stream.
   *
   * @param params The params needed to send.
   * @return A class contains the constructed requests and the first param.
   */
  public static WsParam prepareWsParams(Flowable<Param> params) {
    AtomicInteger integer = new AtomicInteger(0);
    var firstParam = params.blockingFirst();
    var requests =
        params
            .flatMap(
                param -> {
                  if (integer.getAndIncrement() == 0) {
                    Request request =
                        prepareRequest(Protocol.WEBSOCKET, param, WebSocketEventType.RUN_TASK);
                    if (request.getBinary() != null) {
                      return Flowable.fromArray(
                          Request.builder().message(request.getMessage()).build(),
                          Request.builder().binary(request.getBinary()).build());
                    } else {
                      return Flowable.fromArray(request);
                    }
                  } else {
                    return Flowable.fromArray(
                        prepareRequest(
                            Protocol.WEBSOCKET, param, WebSocketEventType.CONTINUE_TASK));
                  }
                })
            .concatWith(
                Flowable.defer(
                    () ->
                        Flowable.just(
                            prepareRequest(
                                Protocol.WEBSOCKET, firstParam, WebSocketEventType.FINISH_TASK))));
    return WsParam.builder().requests(requests).firstParam(firstParam).build();
  }

  /**
   * Prepare the Result by the response and the input param.
   *
   * @param protocol The protocol.
   * @param param The input param to call `returnType`
   * @param res The response.
   * @return The constructed Result instance.
   */
  public static Result prepareResult(Protocol protocol, Param param, Response res) {
    return prepareResult(protocol, res, param.resultType());
  }

  private static Result prepareResult(
      Protocol protocol, Response res, Class<? extends Result> clazz) {
    try {
      Result result = clazz.newInstance();
      Response response = prepareResponsePayload(protocol, res);
      if (Protocol.WEBSOCKET == protocol) {
        result.setRequestId(response.getHeaders().get(TASKID));
      } else {
        JsonObject jsonObject = JsonUtils.parse(response.getMessage());
        result.setRequestId(
            jsonObject.get(REQUEST_ID) == null ? null : jsonObject.get(REQUEST_ID).getAsString());
      }
      result.fromResponse(protocol, response);
      return result;
    } catch (Exception e) {
      throw new ApiException(e);
    }
  }

  /**
   * Call rpc on single-in, stream-out mode
   *
   * @param protocol The protocol.
   * @param url The url.
   * @param headers The headers.
   * @param request The input request.
   * @param receiveTimeout The timeout of receiving single message.
   * @param httpMethod The `HttpMethod` to use if the protocol is http.
   * @param mode The streaming mode.
   * @return The Flowable of `Response`
   */
  public static Flowable<Response> streamingOut(
      Protocol protocol,
      String url,
      Map<String, String> headers,
      Request request,
      HttpMethod httpMethod,
      StreamingMode mode,
      long receiveTimeout)
      throws ApiException {
    return Flowable.<Response>create(
            emitter ->
                ServiceFacility.doSend(emitter, protocol, url, headers, request, httpMethod, mode),
            BackpressureStrategy.BUFFER)
        .filter(
            message ->
                protocol == Protocol.HTTP
                    || message.getMessage() == null
                    || !isStartEvent(message.getMessage()))
        .timeout(receiveTimeout, TimeUnit.SECONDS);
  }

  /**
   * Call rpc on single-in, single-out mode
   *
   * @param protocol The protocol.
   * @param url The url.
   * @param headers The headers.
   * @param request The input request.
   * @param receiveTimeout The timeout of receiving single message.
   * @param httpMethod The `HttpMethod` to use if the protocol is http.
   * @return The Flowable of `Response`
   */
  public static Response streamingNone(
      Protocol protocol,
      String url,
      Map<String, String> headers,
      Request request,
      HttpMethod httpMethod,
      long receiveTimeout)
      throws ApiException {
    return streamingOut(
            protocol, url, headers, request, httpMethod, StreamingMode.NONE, receiveTimeout)
        .filter(
            message ->
                protocol == Protocol.HTTP
                    || message.getMessage() == null
                    || !isStartEvent(message.getMessage()))
        .blockingFirst();
  }

  /**
   * Call rpc on single-in mode with a callback.
   *
   * @param protocol The protocol.
   * @param url The url.
   * @param headers The headers.
   * @param request The input request.
   * @param mode The streaming mode.
   * @param httpMethod The `HttpMethod` to use if the protocol is http.
   * @param callback The custom callback to use.
   */
  public static void streamingOutWithCallback(
      Protocol protocol,
      String url,
      Map<String, String> headers,
      Request request,
      HttpMethod httpMethod,
      StreamingMode mode,
      ResultCallback<Response> callback) {
    var e = checkApiKey(headers);
    if (e != null) {
      callback.onError(e);
      return;
    }
    switch (protocol) {
      case HTTP:
        HttpRpc httpRpc =
            new HttpRpc(url, new RpcResponseCallback<>(callback, input -> input, null));
        switch (httpMethod) {
          case GET:
            httpRpc.get(headers);
            break;
          case POST:
            if (mode == StreamingMode.NONE) {
              httpRpc.post(headers, request);
            } else {
              httpRpc.sse(headers, request);
            }
            break;
          default:
            callback.onError(
                new ApiException(
                    Status.builder()
                        .statusCode(HttpStatus.SC_BAD_REQUEST)
                        .code(ErrorType.PROTOCOL_UNSUPPORTED.getValue())
                        .message("Invalid Http Method: " + httpMethod)
                        .build()));
            break;
        }
        break;
      case WEBSOCKET:
        try {
          WebsocketRpc websocketRpc = WebsocketPool.getInstance().getWsClient(url, headers);
          websocketRpc.call(
              request,
              new RpcResponseCallback<>(callback, input -> input, websocketRpc),
              ServiceFacility::isFinishEvent);
        } catch (Exception ex) {
          callback.onError(ex instanceof ApiException ? ex : new ApiException(ex));
        }
        break;
    }
  }

  /**
   * Call rpc with a callback on stream-in mode, only support websocket.
   *
   * @param url The url.
   * @param headers The headers.
   * @param requests The iterable input request.
   * @param callback The custom callback to use.
   */
  public static void streamingInAndDuplexWithCallback(
      String url,
      Map<String, String> headers,
      Flowable<Request> requests,
      ResultCallback<Response> callback) {
    var e = checkApiKey(headers);
    if (e != null) {
      callback.onError(e);
      return;
    }
    try {
      WebsocketRpc websocketRpc = WebsocketPool.getInstance().getWsClient(url, headers);
      websocketRpc.call(
          requests,
          new RpcResponseCallback<>(callback, input -> input, websocketRpc),
          ServiceFacility::isFinishEvent);
    } catch (Exception ex) {
      callback.onError(ex instanceof ApiException ? ex : new ApiException(ex));
    }
  }

  /**
   * Call rpc on stream-in mode, only support websocket.
   *
   * @param url The url.
   * @param headers The headers.
   * @param requests The iterable input request.
   * @param receiveTimeout The message timeout.
   * @return A Flowable of `Response`
   */
  public static Flowable<Response> streamingInAndDuplex(
      String url, Map<String, String> headers, Flowable<Request> requests, long receiveTimeout) {
    return Flowable.<Response>create(
            emitter -> {
              var e = checkApiKey(headers);
              if (e != null) {
                emitter.onError(e);
                return;
              }
              try {
                WebsocketRpc websocketRpc = WebsocketPool.getInstance().getWsClient(url, headers);
                websocketRpc.call(
                    requests,
                    new RpcResponseCallback<>(emitter, input -> input, websocketRpc),
                    ServiceFacility::isFinishEvent);
              } catch (Exception ex) {
                emitter.onError(ex instanceof ApiException ? ex : new ApiException(ex));
              }
            },
            BackpressureStrategy.BUFFER)
        .filter(message -> message.getMessage() == null || !isStartEvent(message.getMessage()))
        .timeout(receiveTimeout, TimeUnit.SECONDS);
  }

  private static boolean isEvent(String msg, WebSocketEventType eventType) {
    if (msg == null) {
      return false;
    }
    JsonObject jsonObject = JsonUtils.parse(msg);
    JsonObject header = jsonObject.getAsJsonObject(HEADER);
    return header != null && eventType.getValue().equals(header.get(EVENT).getAsString());
  }

  public static boolean isFinishEvent(String msg) {
    return isEvent(msg, WebSocketEventType.TASK_FINISHED);
  }

  public static boolean isStartEvent(String msg) {
    return isEvent(msg, WebSocketEventType.TASK_STARTED);
  }

  public static boolean isFailedEvent(String msg) {
    return isEvent(msg, WebSocketEventType.TASK_FAILED);
  }

  private static Status parseStatus(Status status) {
    if (status.isJson()) {
      JsonObject json = JsonUtils.parse(status.getMessage());
      return Status.builder()
          .code(json.get(CODE) == null ? null : json.get(CODE).getAsString())
          .statusCode(status.getStatusCode())
          .message(json.get(MESSAGE) == null ? null : json.get(MESSAGE).getAsString())
          .usage(json.getAsJsonObject(USAGE))
          .requestId(json.get(REQUEST_ID) == null ? null : json.get(REQUEST_ID).getAsString())
          .isJson(false)
          .build();
    }
    return status;
  }

  private static ApiException checkApiKey(Map<String, String> headers) {
    if (headers == null || headers.get("Authorization") == null) {
      return new ApiException(
          Status.builder()
              .statusCode(HttpStatus.SC_UNAUTHORIZED)
              .code(ErrorType.API_KEY_ERROR.getValue())
              .message(ErrorType.API_KEY_ERROR.getValue())
              .build());
    }
    return null;
  }

  private static Exception parseThrowable(Throwable e) {
    if (e instanceof ApiException) {
      var apiException = (ApiException) e;
      apiException.setStatus(parseStatus(apiException.getStatus()));
      return apiException;
    }
    return new ApiException(e);
  }

  private static void doSend(
      Emitter<Response> emitter,
      Protocol protocol,
      String url,
      Map<String, String> headers,
      Request request,
      HttpMethod httpMethod,
      StreamingMode mode) {
    var e = checkApiKey(headers);
    if (e != null) {
      emitter.onError(e);
      return;
    }
    switch (protocol) {
      case HTTP:
        HttpRpc httpRpc =
            new HttpRpc(url, new RpcResponseCallback<>(emitter, input -> input, null));
        switch (httpMethod) {
          case GET:
            httpRpc.get(headers);
            break;
          case POST:
            if (mode == StreamingMode.NONE) {
              httpRpc.post(headers, request);
            } else {
              httpRpc.sse(headers, request);
            }
            break;
          default:
            emitter.onError(
                new ApiException(
                    Status.builder()
                        .statusCode(HttpStatus.SC_BAD_REQUEST)
                        .code(ErrorType.PROTOCOL_UNSUPPORTED.getValue())
                        .message("Invalid Http Method: " + httpMethod)
                        .build()));
            break;
        }
        break;
      case WEBSOCKET:
        try {
          WebsocketRpc websocketRpc = WebsocketPool.getInstance().getWsClient(url, headers);
          websocketRpc.call(
              request,
              new RpcResponseCallback<>(emitter, input -> input, websocketRpc),
              ServiceFacility::isFinishEvent);
        } catch (Exception ex) {
          emitter.onError(ex instanceof ApiException ? ex : new ApiException(ex));
        }
        break;
    }
  }

  private static Response prepareResponsePayload(Protocol protocol, Response response) {
    if (protocol == Protocol.WEBSOCKET) {
      if (response.getMessage() != null) {
        JsonObject jsonObject = JsonUtils.parse(response.getMessage());
        JsonObject header = jsonObject.getAsJsonObject(HEADER);
        Map<String, String> map = Maps.newHashMap();
        for (Map.Entry<String, JsonElement> entry : header.entrySet()) {
          map.put(entry.getKey(), entry.getValue().getAsString());
        }
        response.setHeaders(map);
        response.setMessage(JsonUtils.toJson(jsonObject.getAsJsonObject(PAYLOAD)));
      }
    }
    return response;
  }

  static class RpcResponseCallback<T> extends ResultCallback<Response> {

    private final Emitter<T> emitter;

    private final ResultCallback<T> callback;

    private volatile WebsocketRpc websocketRpc;

    private final Function<Response, T> converter;

    public RpcResponseCallback(
        Emitter<T> emitter, Function<Response, T> converter, WebsocketRpc websocketRpc) {
      this.emitter = emitter;
      this.callback = null;
      this.converter = converter;
      this.websocketRpc = websocketRpc;
    }

    public RpcResponseCallback(
        ResultCallback<T> callback, Function<Response, T> converter, WebsocketRpc websocketRpc) {
      this.callback = callback;
      this.emitter = null;
      this.converter = converter;
      this.websocketRpc = websocketRpc;
    }

    public void onOpen(Status status) {
      if (callback != null) {
        callback.onOpen(parseStatus(status));
      }
    }

    @Override
    public void onEvent(Response message) {
      if (isFailedEvent(message.getMessage())) {
        JsonObject jsonObject = JsonUtils.parse(message.getMessage());
        WsHeader wsHeader =
            JsonUtils.fromJsonObject(jsonObject.getAsJsonObject(HEADER), WsHeader.class);
        JsonObject payload = jsonObject.getAsJsonObject(PAYLOAD);
        JsonObject usage = payload == null ? null : payload.getAsJsonObject(USAGE);
        Status status =
            Status.builder()
                .message(wsHeader.getErrorMessage())
                .statusCode(44)
                .code(wsHeader.getErrorName())
                .requestId(wsHeader.getTaskId())
                .usage(usage)
                .build();
        if (emitter != null) {
          emitter.onError(new ApiException(status));
        } else {
          assert callback != null;
          callback.onError(new ApiException(status));
        }
      } else {
        try {
          T result = converter.apply(message);
          if (emitter != null) {
            emitter.onNext(result);
          } else {
            assert callback != null;
            callback.onEvent(result);
          }
        } catch (Exception e) {
          if (emitter != null) {
            emitter.onError(e instanceof ApiException ? e : new ApiException(e));
          } else {
            assert callback != null;
            callback.onError(e instanceof ApiException ? e : new ApiException(e));
          }
        }
      }
    }

    @Override
    public void onComplete() {
      if (emitter != null) {
        emitter.onComplete();
      } else {
        assert callback != null;
        callback.onComplete();
      }
      if (this.websocketRpc != null) {
        synchronized (this) {
          if (this.websocketRpc != null) {
            WebsocketPool.getInstance().returnWsClient(this.websocketRpc);
            this.websocketRpc = null;
          }
        }
      }
    }

    @Override
    protected void finalize() throws Throwable {
      if (this.websocketRpc != null) {
        synchronized (this) {
          if (this.websocketRpc != null) {
            WebsocketPool.getInstance().returnWsClient(this.websocketRpc);
            this.websocketRpc = null;
          }
        }
      }
      super.finalize();
    }

    @Override
    public void onError(Exception e) {
      if (emitter != null) {
        emitter.onError(parseThrowable(e));
      } else {
        assert callback != null;
        callback.onError(parseThrowable(e));
      }
    }

    public void onClose(Status status) {
      if (callback != null) {
        callback.onClose(parseStatus(status));
      }
    }

    public void doClose(Status status) {
      if (callback != null) {
        callback.doClose(parseStatus(status));
      }
    }
  }
}
