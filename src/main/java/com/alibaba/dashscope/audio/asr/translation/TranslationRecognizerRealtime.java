// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.translation;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResult;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResultPack;
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
import lombok.val;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class TranslationRecognizerRealtime {
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

  /** Gummy Translation and Recognition real-time SDK. */
  public TranslationRecognizerRealtime() {
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
   * Gummy Translation and Recognition real-time SDK.
   *
   * @param baseUrl Base URL
   */
  public TranslationRecognizerRealtime(String baseUrl) {
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
   * Gummy Translation and Recognition real-time SDK.
   *
   * @param baseUrl Base URL
   * @param connectionOptions Connection options
   */
  public TranslationRecognizerRealtime(String baseUrl, ConnectionOptions connectionOptions) {
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
   * Speech Translation and Recognition real-time using Flowable features
   *
   * @param param Configuration for speech translation and recognition, including audio format,
   *     source language, target languages, etc.
   * @param audioFrame The audio stream to be recognized
   * @return The output event stream, including real-time asr and translation results and timestamps
   */
  public Flowable<TranslationRecognizerResult> streamCall(
      TranslationRecognizerParam param, Flowable<ByteBuffer> audioFrame)
      throws ApiException, NoApiKeyException {
    this.reset();
    preRequestId = UUID.randomUUID().toString();
    return duplexApi
        .duplexCall(
            TranslationRecognizerParamWithStream.FromTranslationRecognizerParam(
                param,
                audioFrame.doOnNext(
                    buffer -> {
                      if (startStreamTimeStamp < 0) {
                        startStreamTimeStamp = System.currentTimeMillis();
                      }
                      log.debug("send audio frame: " + buffer.remaining());
                    }),
                    preRequestId))
        .doOnComplete(
            () -> {
              this.stopStreamTimeStamp = System.currentTimeMillis();
            })
        .map(
            item -> {
              return TranslationRecognizerResult.fromDashScopeResult(item);
            })
        .filter(
            item ->
                item != null
                    && (item.getTranslationResult() != null
                        || item.getTranscriptionResult() != null)
                    && !item.isCompleteResult())
        .doOnNext(
            result -> {
              if (firstPackageTimeStamp < 0) {
                firstPackageTimeStamp = System.currentTimeMillis();
                log.debug("first package delay: " + getFirstPackageDelay());
              }
              log.debug(
                  "[Recv Result] transcription: "
                      + result.getTranslationResult()
                      + " translation: "
                      + result.getTranscriptionResult());
            })
        .doOnComplete(
            () -> {
              onCompleteTimeStamp = System.currentTimeMillis();
              log.debug("last package delay: " + getLastPackageDelay());
            });
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
              synchronized (TranslationRecognizerRealtime.this) {
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
          TranslationRecognizerParamWithStream.FromTranslationRecognizerParam(param, audioFrames, preRequestId),
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
                callback.onEvent(translationRecognizerResult);
              }
            }

            @Override
            public void onComplete() {
              onCompleteTimeStamp = System.currentTimeMillis();
              log.debug("last package delay: " + getLastPackageDelay());
              synchronized (TranslationRecognizerRealtime.this) {
                state = TranslationRecognizerState.IDLE;
              }
              callback.onComplete();
              if (stopLatch.get() != null) {
                stopLatch.get().countDown();
              }
            }

            @Override
            public void onError(Exception e) {
              synchronized (TranslationRecognizerRealtime.this) {
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
    log.debug("TranslationRecognizerRealtime started");
  }

  /**
   * Speech Translation and Recognition real-time from local file.
   *
   * @param param Configuration for speech translation and recognition, including audio format,
   *     source language, target languages, etc.
   * @param file Local audio file
   */
  public TranslationRecognizerResultPack call(TranslationRecognizerParam param, File file) {
    this.reset();
    if (param == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: TranslationRecognizerParam is null"));
    }
    if (file == null || !file.canRead()) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: Input file is null or not exists"));
    }

    startStreamTimeStamp = System.currentTimeMillis();

    AtomicBoolean cancel = new AtomicBoolean(false);
    TranslationRecognizerResultPack results = new TranslationRecognizerResultPack();
    Flowable<ByteBuffer> audioFrames =
        Flowable.create(
            emitter -> {
              new Thread(
                      () -> {
                        try {
                          try (val channel = new FileInputStream(file).getChannel()) {
                            ByteBuffer buffer = ByteBuffer.allocate(4096 * 4);
                            while (channel.read(buffer) != -1 && !cancel.get()) {
                              buffer.flip();
                              emitter.onNext(buffer);
                              buffer = ByteBuffer.allocate(4096 * 4);
                              Thread.sleep(100);
                            }
                          }
                          emitter.onComplete();
                          this.stopStreamTimeStamp = System.currentTimeMillis();
                        } catch (Exception e) {
                          emitter.onError(e);
                        }
                      })
                  .start();
            },
            BackpressureStrategy.BUFFER);
    preRequestId = UUID.randomUUID().toString();
    try {
      duplexApi
          .duplexCall(
              TranslationRecognizerParamWithStream.FromTranslationRecognizerParam(
                  param, audioFrames, preRequestId))
          .doOnComplete(
              () -> {
                onCompleteTimeStamp = System.currentTimeMillis();
                log.debug("last package delay: " + getLastPackageDelay());
              })
          .blockingSubscribe(
              res -> {
                TranslationRecognizerResult translationRecognizerResult =
                    TranslationRecognizerResult.fromDashScopeResult(res);
                if (!translationRecognizerResult.isCompleteResult()
                    && translationRecognizerResult.isSentenceEnd()) {
                  if (firstPackageTimeStamp < 0) {
                    firstPackageTimeStamp = System.currentTimeMillis();
                    log.debug("first package delay: " + getFirstPackageDelay());
                  }
                  log.debug(
                      "[Recv SentenceEnd]: transcription"
                          + translationRecognizerResult.getTranslationResult()
                          + ", translation: "
                          + translationRecognizerResult.getTranscriptionResult());
                  results.setRequestId(translationRecognizerResult.getRequestId());
                  results
                      .getTranslationResultList()
                      .add(translationRecognizerResult.getTranslationResult());
                  results
                      .getTranscriptionResultList()
                      .add(translationRecognizerResult.getTranscriptionResult());
                  results.getUsageList().add(translationRecognizerResult.getUsage());
                }
              },
              e -> {
                results.setError(e);
                cancel.set(true);
              },
              () -> {});
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
    if (results.getError() != null) {
      ApiException apiException = new ApiException(results.getError());
      apiException.setStackTrace(results.getError().getStackTrace());
      throw apiException;
    }
    return results;
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
  }

  /**
   * Stop Speech Translation and Recognition real-time via call and sendAudioFrame API. The correct
   * order of calls is: first call, then repeatedly sendAudioFrame, and finally stop.
   */
  public void stop() {
    this.stopStreamTimeStamp = System.currentTimeMillis();
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
        stopLatch.get().await();
      } catch (InterruptedException ignored) {
      }
    }
  }

  /** Reset SDK, should be called before reuse SDK object. */
  private void reset() {
    this.audioEmitter = null;
    this.cmdBuffer.clear();
    this.state = TranslationRecognizerState.IDLE;
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
