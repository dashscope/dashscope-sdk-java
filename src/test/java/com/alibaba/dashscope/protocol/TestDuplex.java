// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import static com.alibaba.dashscope.utils.ApiKeywords.HEADER;
import static com.alibaba.dashscope.utils.ApiKeywords.PAYLOAD;

import com.alibaba.dashscope.aigc.conversation.ConversationMessagePayload;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.common.WebSocketEventType;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.mock.MockServer;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestDuplex {

  private final MockServer mockServer = new MockServer();

  private String oldBaseHttpApiUrl = null;

  private String oldBaseWebsocketApiUrl = null;

  @BeforeEach
  public void setUp() {
    oldBaseHttpApiUrl = Constants.baseHttpApiUrl;
    oldBaseWebsocketApiUrl = Constants.baseWebsocketApiUrl;
    Constants.baseHttpApiUrl = "http://localhost:8080/api/v1";
    Constants.baseWebsocketApiUrl = "ws://localhost:8080/api-ws/v1/inference";
    mockServer.start(0, "testKey");
  }

  @AfterEach
  public void tearDown() {
    Constants.baseHttpApiUrl = oldBaseHttpApiUrl;
    Constants.baseWebsocketApiUrl = oldBaseWebsocketApiUrl;
    mockServer.tearDown();
  }

  @Test
  public void testDuplexAndBinary() {
    ByteBuffer[] byteBuffers = new ByteBuffer[2];
    for (int i = 0; i < 2; ++i) {
      byteBuffers[i] = ByteBuffer.wrap("testBinary".getBytes());
    }
    String requestId = UUID.randomUUID().toString();
    AtomicReference<Emitter<Request>> atomicReference = new AtomicReference<>();
    Flowable<Request> requests =
        Flowable.<Request>create(
                emitter -> {
                  atomicReference.set(emitter);
                  WsHeader header =
                      WsHeader.buildInputHeader(
                          requestId, StreamingMode.DUPLEX, WebSocketEventType.RUN_TASK);
                  Map<String, Object> map = Maps.newHashMap();
                  map.put("header", header);
                  map.put(
                      "payload",
                      ConversationMessagePayload.buildInputPayload("test", new JsonObject()));
                  emitter.onNext(Request.builder().message(JsonUtils.toJson(map)).build());
                },
                BackpressureStrategy.BUFFER)
            .timeout(30, TimeUnit.SECONDS);

    Map<String, String> headers = Maps.newHashMap();
    headers.put("Authorization", "testKey");
    AtomicInteger atomicInteger = new AtomicInteger(0);
    ServiceFacility.streamingInAndDuplexWithCallback(
        Constants.baseWebsocketApiUrl,
        headers,
        requests,
        new ResultCallback<Response>() {

          @Override
          public void onEvent(Response message) {
            if (message.getBinary() != null) {
              Charset charset = StandardCharsets.UTF_8; // Specify the character encoding
              CharBuffer charBuffer = charset.decode(message.getBinary());
              assert (charBuffer.toString().equals("testBinary"));
            } else if (message.getMessage() != null) {
              assert (message.getMessage().contains(WebSocketEventType.TASK_FINISHED.getValue())
                  || message.getMessage().contains(WebSocketEventType.TASK_STARTED.getValue()));
            } else {
              throw new RuntimeException();
            }
            if (message.getBinary() != null
                || message.getMessage().contains(WebSocketEventType.TASK_STARTED.getValue())) {
              if (atomicInteger.get() < 2) {
                atomicReference
                    .get()
                    .onNext(
                        Request.builder()
                            .binary(byteBuffers[atomicInteger.getAndIncrement()])
                            .build());
              } else {
                WsHeader header1 =
                    WsHeader.buildInputHeader(
                        requestId, StreamingMode.DUPLEX, WebSocketEventType.FINISH_TASK);
                Map<String, Object> map1 = Maps.newHashMap();
                map1.put(HEADER, header1);
                map1.put(
                    PAYLOAD,
                    ConversationMessagePayload.buildInputPayload("test", new JsonObject()));
                atomicReference
                    .get()
                    .onNext(Request.builder().message(JsonUtils.toJson(map1)).build());
                atomicReference.get().onComplete();
              }
            }
          }

          @Override
          public void onError(Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @SuperBuilder
  static class TestParam extends Param {

    @Override
    public String url() {
      return null;
    }

    @Override
    public Request toRequest(Protocol protocol) {
      return Request.builder().binary(ByteBuffer.wrap("testBinary".getBytes())).build();
    }

    @Override
    public Class<? extends Result> resultType() {
      return TestResult.class;
    }
  }

  static class TestResult extends Result {

    String message;

    @Override
    public void fromResponse(Protocol protocol, Response response) throws ApiException {
      if (response.getBinary() != null) {
        Charset charset = StandardCharsets.UTF_8; // Specify the character encoding
        CharBuffer charBuffer = charset.decode(response.getBinary());
        message = charBuffer.toString();
      }
    }
  }

  @Test
  public void testDuplexAndBinaryHighLevelApi() {
    Flowable<Param> requests =
        Flowable.fromArray(
            TestParam.builder().apiKey("testKey").mode(StreamingMode.DUPLEX).model("test").build(),
            TestParam.builder().apiKey("testKey").mode(StreamingMode.DUPLEX).model("test").build());
    Flowable<Result> results = ServiceFacility.streamCall(new HashMap<>(), requests, 30);
    AtomicInteger integer = new AtomicInteger(0);
    results.blockingForEach(
        result -> {
          assert integer.getAndIncrement() >= 2
              || (((TestResult) result).message.equals("testBinary"));
        });
  }

  @Test
  public void testDuplexAndBinaryHighLevelApiCallback() {
    Flowable<Param> requests =
        Flowable.fromArray(
            TestParam.builder().apiKey("testKey").mode(StreamingMode.DUPLEX).model("test").build(),
            TestParam.builder().apiKey("testKey").mode(StreamingMode.DUPLEX).model("test").build());
    AtomicInteger integer = new AtomicInteger(0);
    ServiceFacility.streamCall(
        new HashMap<>(),
        requests,
        new ResultCallback<Result>() {
          @Override
          public void onEvent(Result message) {
            assert integer.getAndIncrement() >= 2
                || (((TestResult) message).message.equals("testBinary"));
          }
        });
  }
}
