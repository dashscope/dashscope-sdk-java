// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.audio.asr.recognition.timestamp.Sentence;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class Recognition {
  @Getter SynchronizeFullDuplexApi<RecognitionParamWithStream> duplexApi;

  private ApiServiceOption serviceOption;

  private Emitter<ByteBuffer> audioEmitter;

  @SuperBuilder
  private static class AsyncCmdBuffer {
    @Builder.Default private boolean isStop = false;
    private ByteBuffer audioFrame;
  }

  private final Queue<AsyncCmdBuffer> cmdBuffer = new LinkedList<>();

  private RecognitionState state = RecognitionState.IDLE;

  private AtomicReference<CountDownLatch> stopLatch = new AtomicReference<>(null);

  private long startStreamTimeStamp = -1;
  private long firstPackageTimeStamp = -1;

  @SuperBuilder
  private static class RecognitionParamWithStream extends RecognitionParam {

    @NonNull private Flowable<ByteBuffer> audioStream;

    @Override
    public Flowable<Object> getStreamingData() {
      return audioStream.cast(Object.class);
    }

    public static RecognitionParamWithStream FromRecognitionParam(
        RecognitionParam param, Flowable<ByteBuffer> audioStream) {
      RecognitionParamWithStream recognitionParamWithStream =
          RecognitionParamWithStream.builder()
              .parameters((param.getParameters()))
              .headers(param.getHeaders())
              .format(param.getFormat())
              .audioStream(audioStream)
              .disfluencyRemovalEnabled(param.isDisfluencyRemovalEnabled())
              .model(param.getModel())
              .sampleRate(param.getSampleRate())
              .apiKey(param.getApiKey())
              .build();
      if (param.getPhraseId() != null && !param.getPhraseId().isEmpty()) {
        recognitionParamWithStream.setPhraseId(param.getPhraseId());
      }

      return recognitionParamWithStream;
    }
  }

  public Recognition() {
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

  public Flowable<RecognitionResult> streamCall(
      RecognitionParam param, Flowable<ByteBuffer> audioFrame)
      throws ApiException, NoApiKeyException {
    this.reset();
    return duplexApi
        .duplexCall(
            RecognitionParamWithStream.FromRecognitionParam(
                param,
                audioFrame.doOnNext(
                    buffer -> {
                      if (startStreamTimeStamp < 0) {
                        startStreamTimeStamp = System.currentTimeMillis();
                      }
                      log.debug("send audio frame: " + buffer.remaining());
                    })))
        .map(
            item -> {
              return RecognitionResult.fromDashScopeResult(item);
            })
        .filter(item -> item != null && item.getSentence() != null && !item.isCompleteResult())
        .doOnNext(
            result -> {
              if (firstPackageTimeStamp < 0) {
                firstPackageTimeStamp = System.currentTimeMillis();
                log.debug(
                    "first package delay: "
                        + (System.currentTimeMillis() - startStreamTimeStamp)
                        + " ms");
              }
              log.debug(
                  "Recv Result: "
                      + result.getSentence().getText()
                      + ", isEnd: "
                      + result.isSentenceEnd());
            })
        .doOnComplete(() -> {});
  }

  public void call(RecognitionParam param, ResultCallback<RecognitionResult> callback) {
    this.reset();
    if (param == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: RecognitionParam is null"));
    }

    if (callback == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: ResultCallback is null"));
    }

    Flowable<ByteBuffer> audioFrames =
        Flowable.create(
            emitter -> {
              synchronized (Recognition.this) {
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
      state = RecognitionState.RECOGNITION_STARTED;
      cmdBuffer.clear();
    }
    stopLatch = new AtomicReference<>(new CountDownLatch(1));

    try {
      duplexApi.duplexCall(
          RecognitionParamWithStream.FromRecognitionParam(param, audioFrames),
          new ResultCallback<DashScopeResult>() {
            @Override
            public void onEvent(DashScopeResult message) {
              RecognitionResult recognitionResult = RecognitionResult.fromDashScopeResult(message);
              if (!recognitionResult.isCompleteResult()) {
                if (firstPackageTimeStamp < 0) {
                  firstPackageTimeStamp = System.currentTimeMillis();
                  log.debug(
                      "first package delay: "
                          + (System.currentTimeMillis() - startStreamTimeStamp)
                          + " ms");
                }
                log.debug(
                    "Recv Result: "
                        + recognitionResult.getSentence().getText()
                        + ", isEnd: "
                        + recognitionResult.isSentenceEnd());
                callback.onEvent(recognitionResult);
              }
            }

            @Override
            public void onComplete() {
              synchronized (Recognition.this) {
                state = RecognitionState.IDLE;
              }
              callback.onComplete();
              if (stopLatch.get() != null) {
                stopLatch.get().countDown();
              }
            }

            @Override
            public void onError(Exception e) {
              synchronized (Recognition.this) {
                state = RecognitionState.IDLE;
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
    log.info("Recognition started");
  }

  public String call(RecognitionParam param, File file) {
    this.reset();
    if (param == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: RecognitionParam is null"));
    }
    if (file == null || !file.canRead()) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: Input file is null or not exists"));
    }

    startStreamTimeStamp = System.currentTimeMillis();

    AtomicBoolean cancel = new AtomicBoolean(false);
    AtomicReference<String> finalResult = new AtomicReference<>(null);
    AtomicReference<Throwable> finalError = new AtomicReference<>(null);
    List<Sentence> sentenceList = new ArrayList<>();
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
                        } catch (Exception e) {
                          emitter.onError(e);
                        }
                      })
                  .start();
            },
            BackpressureStrategy.BUFFER);
    try {
      duplexApi
          .duplexCall(RecognitionParamWithStream.FromRecognitionParam(param, audioFrames))
          .blockingSubscribe(
              res -> {
                RecognitionResult recognitionResult = RecognitionResult.fromDashScopeResult(res);
                if (!recognitionResult.isCompleteResult() && recognitionResult.isSentenceEnd()) {
                  if (firstPackageTimeStamp < 0) {
                    firstPackageTimeStamp = System.currentTimeMillis();
                    log.debug(
                        "first package delay: "
                            + (System.currentTimeMillis() - startStreamTimeStamp)
                            + " ms");
                  }
                  log.debug(
                      "Recv Result: "
                          + recognitionResult.getSentence().getText()
                          + ", isEnd: "
                          + recognitionResult.isSentenceEnd());
                  sentenceList.add(recognitionResult.getSentence());
                }
              },
              e -> {
                finalError.set(e);
                cancel.set(true);
              },
              () -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("sentences", new Gson().toJsonTree(sentenceList).getAsJsonArray());
                finalResult.set(jsonObject.toString());
              });
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
    if (finalError.get() != null) {
      ApiException apiException = new ApiException(finalError.get());
      apiException.setStackTrace(finalError.get().getStackTrace());
      throw apiException;
    }
    return finalResult.get();
  }

  public void sendAudioFrame(ByteBuffer audioFrame) {
    if (audioFrame == null) {
      throw new ApiException(new InputRequiredException("Parameter invalid: audioFrame is null"));
    }
    if (this.startStreamTimeStamp < 0) {
      this.startStreamTimeStamp = System.currentTimeMillis();
    }
    log.debug("send audio frame: " + audioFrame.remaining());
    synchronized (this) {
      if (state != RecognitionState.RECOGNITION_STARTED) {
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

  public void stop() {
    synchronized (this) {
      if (state != RecognitionState.RECOGNITION_STARTED) {
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

  private void reset() {
    this.audioEmitter = null;
    this.cmdBuffer.clear();
    this.state = RecognitionState.IDLE;
    this.stopLatch = new AtomicReference<>(null);
    this.startStreamTimeStamp = -1;
    this.firstPackageTimeStamp = -1;
  }
}
