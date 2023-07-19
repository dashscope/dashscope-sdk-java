package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.audio.asr.recognition.timestamp.Sentence;
import com.alibaba.dashscope.common.Param;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.ServiceFacility;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class Recognition implements BaseRecognition {

  @Setter private long responseTimeout = 30;

  private Emitter<ByteBuffer> audioEmitter;

  @SuperBuilder
  private static class AsyncCmdBuffer {
    @Builder.Default private boolean isStop = false;

    private ByteBuffer audioFrame;
  }

  private final Queue<AsyncCmdBuffer> cmdBuffer = new LinkedList<>();

  private RecognitionState state = RecognitionState.IDLE;

  private AtomicReference<CountDownLatch> stopLatch = new AtomicReference<>(null);

  private Flowable<Param> toParam(RecognitionParam param, Flowable<ByteBuffer> audioFrames) {
    RecognitionRequestParam requestParam =
        RecognitionRequestParam.builder()
            .mode(StreamingMode.DUPLEX)
            .model(param.getModel())
            .format(param.getFormat())
            .sampleRate(param.getSampleRate())
            .disfluencyRemovalEnabled(param.isDisfluencyRemovalEnabled())
            .build();
    if (param.getApiKey() != null) {
      requestParam.setApiKey(param.getApiKey());
    }

    Flowable<Param> params = Flowable.just(requestParam);
    if (audioFrames != null) {
      Flowable<RecognitionBaseParam> streamParams =
          audioFrames.map(
              buffer ->
                  RecognitionStreamParam.builder()
                      .mode(StreamingMode.DUPLEX)
                      .audioFrame(buffer)
                      .build());
      return params.concatWith(streamParams);
    } else {
      return params;
    }
  }

  @Override
  public Flowable<RecognitionResult> streamCall(
      RecognitionParam param, Flowable<ByteBuffer> audioFrames) {
    if (param == null) {
      throw new ApiException(new RuntimeException("Parameter invalid: RecognitionParam is null"));
    }
    if (audioFrames == null) {
      throw new ApiException(
          new RuntimeException("Parameter invalid: audioFrames in RecognitionParam is null"));
    }
    return ServiceFacility.streamCall(null, toParam(param, audioFrames), responseTimeout)
        .map(message -> (RecognitionResult) message)
        .filter(
            result -> result != null && result.getSentence() != null && !result.isCompleteResult());
  }

  @Override
  public void call(RecognitionParam param, ResultCallback<RecognitionResult> callback) {
    if (param == null) {
      throw new ApiException(new RuntimeException("Parameter invalid: RecognitionParam is null"));
    }
    if (callback == null) {
      throw new ApiException(new RuntimeException("Parameter invalid: ResultCallback is null"));
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
    new Thread(
            () -> {
              Flowable<Param> params = toParam(param, audioFrames);
              try {
                ServiceFacility.streamCall(null, params, responseTimeout)
                    .blockingSubscribe(
                        res -> {
                          RecognitionResult recognitionResult = (RecognitionResult) res;
                          if (!recognitionResult.isCompleteResult()) {
                            callback.onEvent(recognitionResult);
                          }
                        },
                        e -> {
                          synchronized (Recognition.this) {
                            state = RecognitionState.IDLE;
                          }
                          callback.onError(new ApiException(e));
                          if (stopLatch.get() != null) {
                            stopLatch.get().countDown();
                          }
                        },
                        () -> {
                          synchronized (Recognition.this) {
                            state = RecognitionState.IDLE;
                          }
                          callback.onComplete();
                          if (stopLatch.get() != null) {
                            stopLatch.get().countDown();
                          }
                        });
              } catch (Exception e) {
                callback.onError(e);
                if (stopLatch.get() != null) {
                  stopLatch.get().countDown();
                }
              }
            })
        .start();
  }

  @Override
  public String call(RecognitionParam param, File file) {
    if (param == null) {
      throw new ApiException(new RuntimeException("Parameter invalid: RecognitionParam is null"));
    }
    if (file == null || !file.canRead()) {
      throw new ApiException(
          new RuntimeException("Parameter invalid: Input file is null or not exists"));
    }

    AtomicBoolean cancel = new AtomicBoolean(false);
    AtomicReference<String> finalResult = new AtomicReference<>(null);
    AtomicReference<Throwable> finalError = new AtomicReference<>(null);
    List<Sentence> sentenceList = new ArrayList<>();
    Flowable<ByteBuffer> audioFrames =
        Flowable.create(
            emitter -> {
              try {
                try (val channel = new FileInputStream(file).getChannel()) {
                  ByteBuffer buffer = ByteBuffer.allocate(4096 * 4);
                  while (channel.read(buffer) != -1 && !cancel.get()) {
                    buffer.flip();
                    emitter.onNext(buffer);
                    buffer = ByteBuffer.allocate(4096 * 4);
                    Thread.sleep(10);
                  }
                }
                emitter.onComplete();
              } catch (Exception e) {
                emitter.onError(e);
              }
            },
            BackpressureStrategy.BUFFER);
    audioFrames = audioFrames.subscribeOn(Schedulers.io());
    ServiceFacility.streamCall(null, toParam(param, audioFrames), responseTimeout)
        .blockingSubscribe(
            res -> {
              RecognitionResult result = (RecognitionResult) res;
              if (!result.isCompleteResult() && result.isSentenceEnd()) {
                sentenceList.add(result.getSentence());
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

    if (finalError.get() != null) {
      throw new ApiException(finalError.get());
    }

    return finalResult.get();
  }

  @Override
  public void sendAudioFrame(ByteBuffer audioFrame) {
    if (audioFrame == null) {
      throw new ApiException(new RuntimeException("Parameter invalid: audioFrame is null"));
    }
    synchronized (this) {
      if (state != RecognitionState.RECOGNITION_STARTED) {
        throw new ApiException(
            new RuntimeException(
                "State invalid: expect recognition state is started but " + state.getValue()));
      }
      if (audioEmitter == null) {
        cmdBuffer.add(AsyncCmdBuffer.builder().audioFrame(audioFrame).build());
      } else {
        audioEmitter.onNext(audioFrame);
      }
    }
  }

  @Override
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
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
