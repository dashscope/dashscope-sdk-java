// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.translation;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResult;
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
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TranslationRecognizerChat {
  @Getter SynchronizeFullDuplexApi<TranslationRecognizerParamWithStream> duplexApi;

  private ApiServiceOption serviceOption;

  private Emitter<ByteBuffer> audioEmitter;

  @SuperBuilder
  private static class AsyncCmdBuffer {
    @Builder.Default private boolean isStop = false;
    private ByteBuffer audioFrame;
  }

  private final Queue<AsyncCmdBuffer> cmdBuffer = new LinkedList<>();

  private TranslationRecognizerState state = TranslationRecognizerState.IDLE;

  private AtomicReference<CountDownLatch> stopLatch = new AtomicReference<>(null);

  private long startStreamTimeStamp = -1;
  private long firstPackageTimeStamp = -1;
  private long stopStreamTimeStamp = -1;
  private long onCompleteTimeStamp = -1;
  private AtomicBoolean isSentenceEnd = new AtomicBoolean(false);
  private AtomicBoolean stopped = new AtomicBoolean(false);
  private String preRequestId = null;

  @SuperBuilder
  private static class TranslationRecognizerParamWithStream extends TranslationRecognizerParam {

    @NonNull private Flowable<ByteBuffer> audioStream;

    @Override
    public Flowable<Object> getStreamingData() {
      return audioStream.cast(Object.class);
    }

    public static TranslationRecognizerParamWithStream FromTranslationRecognizerParam(
        TranslationRecognizerParam param, Flowable<ByteBuffer> audioStream, String preRequestId) {
      TranslationRecognizerParamWithStream translationRecognizerParamWithStream =
          TranslationRecognizerParamWithStream.builder()
              .parameters((param.getParameters()))
              .parameter("pre_task_id", preRequestId)
              .headers(param.getHeaders())
              .format(param.getFormat())
              .audioStream(audioStream)
              .disfluencyRemovalEnabled(param.isDisfluencyRemovalEnabled())
              .model(param.getModel())
              .sampleRate(param.getSampleRate())
              .apiKey(param.getApiKey())
              .build();
      if (param.getPhraseId() != null && !param.getPhraseId().isEmpty()) {
        translationRecognizerParamWithStream.setPhraseId(param.getPhraseId());
      }
      return translationRecognizerParamWithStream;
    }
  }

  /** Gummy Translation and Recognition Chat SDK. */
  public TranslationRecognizerChat() {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.ASR.getValue())
            .function(Function.SPEECH_TRANSLATION.getValue())
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
  }

  /**
   * Gummy Translation and Recognition chat SDK.
   *
   * @param baseUrl Base URL
   */
  public TranslationRecognizerChat(String baseUrl) {
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
    duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
  }

  /**
   * Gummy Translation and Recognition chat SDK.
   *
   * @param baseUrl Base URL
   * @param connectionOptions Connection options
   */
  public TranslationRecognizerChat(String baseUrl, ConnectionOptions connectionOptions) {
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
   * Start Speech Translation and Recognition chat via sendAudioFrame API. The correct order of
   * calls is: first call, then repeatedly sendAudioFrame, and finally stop.
   *
   * @param param Configuration for speech translation and recognition, including audio format,
   *     source language, target languages, etc.
   * @param callback ResultCallback
   */
  public void call(
      TranslationRecognizerParam param, ResultCallback<TranslationRecognizerResult> callback) {
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
              synchronized (TranslationRecognizerChat.this) {
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
      state = TranslationRecognizerState.SPEECH_TRANSLATION_STARTED;
      cmdBuffer.clear();
    }
    stopLatch = new AtomicReference<>(new CountDownLatch(1));

    preRequestId = UUID.randomUUID().toString();

    try {
      duplexApi.duplexCall(
          TranslationRecognizerParamWithStream.FromTranslationRecognizerParam(
              param, audioFrames, preRequestId),
          new ResultCallback<DashScopeResult>() {
            @Override
            public void onEvent(DashScopeResult message) {
              TranslationRecognizerResult translationRecognizerResult =
                  TranslationRecognizerResult.fromDashScopeResult(message);
              if (!translationRecognizerResult.isCompleteResult()) {
                if (firstPackageTimeStamp < 0) {
                  firstPackageTimeStamp = System.currentTimeMillis();
                  log.debug("first package delay: " + getFirstPackageDelay());
                }
                log.debug(
                    "[Recv Result] transcription: "
                        + translationRecognizerResult.getTranslationResult()
                        + " translation: "
                        + translationRecognizerResult.getTranscriptionResult());
                if (translationRecognizerResult.isSentenceEnd()) {
                  log.debug("[Chat] recv sentence end, stop asr");
                  isSentenceEnd.set(true);
                }
                callback.onEvent(translationRecognizerResult);
              }
            }

            @Override
            public void onComplete() {
              onCompleteTimeStamp = System.currentTimeMillis();
              log.debug("last package delay: " + getLastPackageDelay());
              synchronized (TranslationRecognizerChat.this) {
                state = TranslationRecognizerState.IDLE;
              }
              callback.onComplete();
              if (stopLatch.get() != null) {
                stopLatch.get().countDown();
              }
            }

            @Override
            public void onError(Exception e) {
              synchronized (TranslationRecognizerChat.this) {
                state = TranslationRecognizerState.IDLE;
              }
              ApiException apiException = new ApiException(e);
              apiException.setStackTrace(e.getStackTrace());
              callback.onError(apiException);
              if (stopLatch.get() != null) {
                stopLatch.get().countDown();
              }
            }
          });
    } catch (NoApiKeyException e) {
      ApiException apiException = new ApiException(e);
      apiException.setStackTrace(e.getStackTrace());
      callback.onError(apiException);
      if (stopLatch.get() != null) {
        stopLatch.get().countDown();
      }
    }
    log.debug("TranslationRecognizerChat started, state is {}", state.getValue());
  }

  /**
   * Send one frame audio
   *
   * @param audioFrame One frame of binary audio The correct order of calls is: first call, then
   *     repeatedly sendAudioFrame, and finally stop.
   * @return true if send success, false if received sentenceEnd. This chat service will not send
   *     any audio after recognize one sentence. You should stop sending audio and call stop API if
   *     return is false.
   */
  public boolean sendAudioFrame(ByteBuffer audioFrame) {
    if (isSentenceEnd.get()) {
      log.debug("skip audio due to has sentence end");
      return false;
    }
    if (audioFrame == null) {
      throw new ApiException(new InputRequiredException("Parameter invalid: audioFrame is null"));
    }
    if (this.startStreamTimeStamp < 0) {
      this.startStreamTimeStamp = System.currentTimeMillis();
    }
    log.debug("send audio frame: " + audioFrame.remaining());
    synchronized (this) {
      if (state != TranslationRecognizerState.SPEECH_TRANSLATION_STARTED) {
        throw new ApiException(
            new InputRequiredException(
                "State invalid: expect recognition state is started but " + state.getValue()));
      }
      if (audioEmitter == null) {
        cmdBuffer.add(AsyncCmdBuffer.builder().audioFrame(audioFrame).build());
      } else {
        audioEmitter.onNext(audioFrame);
      }
    }
    return true;
  }

  /**
   * Stop Speech Translation and Recognition real-time via call and sendAudioFrame API. The correct
   * order of calls is: first call, then repeatedly sendAudioFrame, and finally stop.
   */
  public void stop() {
    this.stopStreamTimeStamp = System.currentTimeMillis();
    if (stopped.get()) {
      log.debug("TranslationRecognizerChat has already been stopped, skip");
      return;
    }
    log.debug("stop TranslationRecognizerChat");
    synchronized (this) {
      if (state != TranslationRecognizerState.SPEECH_TRANSLATION_STARTED) {
        throw new ApiException(
            new RuntimeException(
                "State invalid: expect recognition state is started but " + state.getValue()));
      }
      if (audioEmitter == null) {
        cmdBuffer.add(AsyncCmdBuffer.builder().isStop(true).build());
      } else {
        audioEmitter.onComplete();
      }
    }

    if (stopLatch.get() != null) {
      try {
        log.debug("start waiting for task-finished");
        stopLatch.get().await();
      } catch (InterruptedException ignored) {
      }
    }
    stopped.set(true);
  }

  /** Reset SDK, should be called before reuse SDK object. */
  private void reset() {
    this.audioEmitter = null;
    this.cmdBuffer.clear();
    this.state = TranslationRecognizerState.IDLE;
    this.stopLatch = new AtomicReference<>(null);
    this.startStreamTimeStamp = -1;
    this.firstPackageTimeStamp = -1;
    this.isSentenceEnd.set(false);
    this.stopped.set(false);
    this.preRequestId = null;
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
