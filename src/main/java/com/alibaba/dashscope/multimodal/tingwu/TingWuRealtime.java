// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.multimodal.tingwu;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class TingWuRealtime {
  @Getter SynchronizeFullDuplexApi<TingWuRealtimeWithStream> duplexApi;

  private ApiServiceOption serviceOption;

  private Emitter<ByteBuffer> audioEmitter;

  @SuperBuilder
  private static class AsyncCmdBuffer {
    @Builder.Default private boolean isStop = false;
    private ByteBuffer audioFrame;
  }

  private final Queue<AsyncCmdBuffer> cmdBuffer = new LinkedList<>();

  private AtomicReference<CountDownLatch> stopLatch = new AtomicReference<>(null);

  private long startStreamTimeStamp = -1;
  private long firstPackageTimeStamp = -1;
  private long stopStreamTimeStamp = -1;
  private long onCompleteTimeStamp = -1;
  private String preRequestId = null;
  private boolean isListenState = false;

  @SuperBuilder
  private static class TingWuRealtimeWithStream extends TingWuRealtimeParam {

    @NonNull private Flowable<ByteBuffer> audioStream;

    @Override
    public Flowable<Object> getStreamingData() {
      return audioStream.cast(Object.class);
    }

    public static TingWuRealtimeWithStream FromTingWuRealtimeParam(
        TingWuRealtimeParam param, Flowable<ByteBuffer> audioStream, String preRequestId) {
      TingWuRealtimeWithStream tingWuRealtimeWithStream =
              TingWuRealtimeWithStream.builder()
              .parameters((param.getParameters()))
//              .parameter("pre_task_id", preRequestId)
              .headers(param.getHeaders())
              .appId(param.getAppId())
              .format(param.getFormat())
              .input(param.getInput())
              .audioStream(audioStream)
              .model(param.getModel())
              .sampleRate(param.getSampleRate())
              .apiKey(param.getApiKey())
              .build();

      tingWuRealtimeWithStream.setDirective("start");
      return tingWuRealtimeWithStream;
    }
  }

  /** Gummy Translation and Recognition real-time SDK. */
  public TingWuRealtime() {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.ASR.getValue())
            .function(Function.RECOGNITION.getValue())
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
  }

  /**
   * Gummy Translation and Recognition real-time SDK.
   *
   * @param baseUrl Base URL
   */
  public TingWuRealtime(String baseUrl) {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AIGC.getValue())
            .task(Task.MULTIMODAL_GENERATION.getValue())
            .function(Function.GENERATION.getValue())
            .baseWebSocketUrl(baseUrl)
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
  }

  /**
   * TingWu Service Realtime API
   *
   * @param baseUrl Base URL
   * @param connectionOptions Connection options
   */
  public TingWuRealtime(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.ASR.getValue())
            .function(Function.SPEECH_TRANSLATION.getValue())
            .baseWebSocketUrl(baseUrl)
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(connectionOptions, serviceOption);
  }

  /**
   * Start Speech Translation and Recognition real-time via sendAudioFrame API. The correct order of
   * calls is: first call, then repeatedly sendAudioFrame, and finally stop.
   *
   * @param param Configuration for speech translation and recognition, including audio format,
   *     source language, target languages, etc.
   * @param callback ResultCallback
   */
  public void call(
      TingWuRealtimeParam param, TingWuRealtimeCallback callback) {
    this.reset();
    if (param == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: TranslationRecognizerParam is null"));
    }

    if (callback == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: ResultCallback is null"));
    }

    Flowable<ByteBuffer> audioFrames =
        Flowable.create(
            emitter -> {
              synchronized (TingWuRealtime.this) {
                if (cmdBuffer.size() > 0) {
                  for (AsyncCmdBuffer buffer : cmdBuffer) {
                    if (buffer.isStop) {
                      emitter.onComplete();
                      return;
                    } else {
                      emitter.onNext(buffer.audioFrame);
                    }
                  }
                  cmdBuffer.clear();
                }
                audioEmitter = emitter;
              }
            },
            BackpressureStrategy.BUFFER);
    synchronized (this) {
      cmdBuffer.clear();
    }
    stopLatch = new AtomicReference<>(new CountDownLatch(1));

//    preRequestId = UUID.randomUUID().toString();
    try {
      duplexApi.duplexCall(
          TingWuRealtimeWithStream.FromTingWuRealtimeParam(
              param, audioFrames, preRequestId),
          new ResultCallback<DashScopeResult>() {
            @Override
            public void onEvent(DashScopeResult message) {
               log.debug("Response Result :" + message);
              TingWuRealtimeResult tingWuRealtimeResult =
                      TingWuRealtimeResult.fromDashScopeResult(message);

              switch (tingWuRealtimeResult.getAction()){
                case "speech-listen":
                  // 建联后收到的第一个服务端返回
                  callback.onStarted(tingWuRealtimeResult.getTaskId());
                  synchronized (TingWuRealtime.this) {
                     isListenState = true;
                  }
                  callback.onSpeechListen(tingWuRealtimeResult.getTaskId(),tingWuRealtimeResult.getOutput().get("dataId").getAsString());
                  break;
                case "task-failed":
                  callback.onError(tingWuRealtimeResult.getOutput().get("errorCode").getAsString(),
                          tingWuRealtimeResult.getOutput().get("errorMessage").getAsString());
                  break;
                case "recognize-result":
                  callback.onRecognizeResult(tingWuRealtimeResult.getTaskId(), tingWuRealtimeResult.getOutput());
                  break;
                case "ai-result":
                  callback.onAiResult(tingWuRealtimeResult.getTaskId(), tingWuRealtimeResult.getOutput());
                  break;
                case "speech-end":
                  callback.onStopped(tingWuRealtimeResult.getTaskId());
              }

            }

            @Override
            public void onComplete() {
              onCompleteTimeStamp = System.currentTimeMillis();
              log.debug("last package delay: " + getLastPackageDelay());
              callback.onClosed();
              if (stopLatch.get() != null) {
                stopLatch.get().countDown();
              }
            }

            @Override
            public void onError(Exception e) {
              ApiException apiException = new ApiException(e);
              apiException.setStackTrace(e.getStackTrace());
              callback.onError(apiException.getStatus().getCode(),apiException.getMessage());
              if (stopLatch.get() != null) {
                stopLatch.get().countDown();
              }
            }
          });
    } catch (NoApiKeyException e) {
      ApiException apiException = new ApiException(e);
      apiException.setStackTrace(e.getStackTrace());
      callback.onError(apiException.getStatus().getCode(),apiException.getMessage());
      if (stopLatch.get() != null) {
        stopLatch.get().countDown();
      }
    }
    log.debug("TingWu Realtime client started");
  }

  /**
   * Send one frame audio
   *
   * @param audioFrame One frame of binary audio The correct order of calls is: first call, then
   *     repeatedly sendAudioFrame, and finally stop.
   */
  public void sendAudioFrame(ByteBuffer audioFrame) {
    if (audioFrame == null) {
      throw new ApiException(new InputRequiredException("Parameter invalid: audioFrame is null"));
    }
    if (this.startStreamTimeStamp < 0) {
      this.startStreamTimeStamp = System.currentTimeMillis();
    }
    log.debug("send audio frame: " + audioFrame.remaining());
    synchronized (this) {
      if (audioEmitter == null || !isListenState ) {
        cmdBuffer.add(AsyncCmdBuffer.builder().audioFrame(audioFrame).build());
      } else {
        audioEmitter.onNext(audioFrame);
      }
    }
  }

  /**
   * Stop Speech Translation and Recognition real-time via call and sendAudioFrame API. The correct
   * order of calls is: first call, then repeatedly sendAudioFrame, and finally stop.
   */
  public void stop() {
    this.stopStreamTimeStamp = System.currentTimeMillis();

    synchronized (this) {
      if (audioEmitter == null) {
        cmdBuffer.add(AsyncCmdBuffer.builder().isStop(true).build());
      } else {
        audioEmitter.onComplete();
      }
    }

    if (stopLatch.get() != null) {
      try {
        stopLatch.get().await();
      } catch (InterruptedException ignored) {
      }
    }
  }

  /** Reset SDK, should be called before reuse SDK object. */
  private void reset() {
    this.audioEmitter = null;
    this.cmdBuffer.clear();
    this.isListenState = false;
    this.stopLatch = new AtomicReference<>(null);
    this.startStreamTimeStamp = -1;
    this.firstPackageTimeStamp = -1;
    this.stopStreamTimeStamp = -1;
    this.onCompleteTimeStamp = -1;
    preRequestId = null;
  }

  /** First Package Delay is the time between start sending audio and receive first words package */
  public long getFirstPackageDelay() {
    return this.firstPackageTimeStamp - this.startStreamTimeStamp;
  }

  /** Last Package Delay is the time between stop sending audio and receive last words package */
  public long getLastPackageDelay() {
    return this.onCompleteTimeStamp - this.stopStreamTimeStamp;
  }

  public String getLastRequestId() {
    return preRequestId;
  }
}
