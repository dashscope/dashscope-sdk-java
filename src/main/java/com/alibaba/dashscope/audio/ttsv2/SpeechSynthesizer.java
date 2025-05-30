// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.ttsv2;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** @author lengjiayi */
@Slf4j
public final class SpeechSynthesizer {
  private final Queue<AsyncCmdBuffer> cmdBuffer = new LinkedList<>();

  @Getter
  //    private final List<Sentence> timestamps = new ArrayList<>(); //
  // Lists.newCopyOnWriteArrayList();
  SynchronizeFullDuplexApi<SpeechSynthesisParam> duplexApi;

  private ApiServiceOption serviceOption;
  private Emitter<String> textEmitter;
  private ResultCallback<SpeechSynthesisResult> callback;
  private SpeechSynthesisState state = SpeechSynthesisState.IDLE;

  private AtomicReference<CountDownLatch> stopLatch = new AtomicReference<>(null);

  private SpeechSynthesisParam parameters;

  @Getter private ByteBuffer audioData;
  private ByteArrayOutputStream outputStream;
  private String preRequestId = null;
  private boolean isFirst = true;
  private AtomicBoolean canceled = new AtomicBoolean(false);
  private boolean asyncCall = false;
  private long startStreamTimeStamp = -1;
  private long firstPackageTimeStamp = -1;
  private double recvAudioLength = 0;

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param param Configuration for speech synthesis, including voice type, volume, etc.
   * @param callback In non-streaming output scenarios, this can be set to null
   * @param baseUrl Base URL
   * @param connectionOptions Connection options
   */
  public SpeechSynthesizer(
      SpeechSynthesisParam param,
      ResultCallback<SpeechSynthesisResult> callback,
      String baseUrl,
      ConnectionOptions connectionOptions) {
    if (param == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: StreamInputTtsParam is null"));
    }

    this.parameters = param;
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.TEXT_TO_SPEECH.getValue())
            .function(Function.SPEECH_SYNTHESIZER.getValue())
            .baseWebSocketUrl(baseUrl)
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(connectionOptions, serviceOption);
    this.callback = callback;
    this.asyncCall = this.callback != null;
  }

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param baseUrl Base URL
   * @param connectionOptions Connection options
   */
  public SpeechSynthesizer(String baseUrl, ConnectionOptions connectionOptions) {
    this.parameters = null;
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.TEXT_TO_SPEECH.getValue())
            .function(Function.SPEECH_SYNTHESIZER.getValue())
            .baseWebSocketUrl(baseUrl)
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(connectionOptions, serviceOption);
    this.callback = null;
  }

  /** CosyVoice Speech Synthesis SDK */
  public SpeechSynthesizer() {
    this.parameters = null;
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.TEXT_TO_SPEECH.getValue())
            .function(Function.SPEECH_SYNTHESIZER.getValue())
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
    this.callback = null;
  }

  public void updateParamAndCallback(
      SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback) {
    this.parameters = param;
    this.callback = callback;

    // reset inner params
    this.stopLatch = new AtomicReference<>(null);
    this.cmdBuffer.clear();
    this.textEmitter = null;
    this.isFirst = true;

    this.asyncCall = this.callback != null;
  }

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param param Configuration for speech synthesis, including voice type, volume, etc.
   * @param callback In non-streaming output scenarios, this can be set to null
   * @param baseUrl Base URL
   */
  public SpeechSynthesizer(
      SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback, String baseUrl) {
    if (param == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: SpeechSynthesisParam is null"));
    }

    this.parameters = param;
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.TEXT_TO_SPEECH.getValue())
            .function(Function.SPEECH_SYNTHESIZER.getValue())
            .baseWebSocketUrl(baseUrl)
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
    this.callback = callback;
    this.asyncCall = this.callback != null;
  }

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param param Configuration for speech synthesis, including voice type, volume, etc.
   * @param callback In non-streaming output scenarios, this can be set to null
   */
  public SpeechSynthesizer(
      SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback) {
    if (param == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: StreamInputTtsParam is null"));
    }

    this.parameters = param;
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.TEXT_TO_SPEECH.getValue())
            .function(Function.SPEECH_SYNTHESIZER.getValue())
            .build();
    duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
    this.callback = callback;
    this.asyncCall = this.callback != null;
  }

  public String getLastRequestId() {
    return preRequestId;
  }

  /**
   * Stream input and output speech synthesis using Flowable features
   *
   * @param textStream The text stream to be synthesized
   * @return The output event stream, including real-time audio and timestamps
   */
  public Flowable<SpeechSynthesisResult> streamingCallAsFlowable(Flowable<String> textStream)
      throws ApiException, NoApiKeyException {
    startStreamTimeStamp = System.currentTimeMillis();
    recvAudioLength = 0;
    preRequestId = UUID.randomUUID().toString();
    return duplexApi
        .duplexCall(
            StreamInputTtsParamWithStream.fromStreamInputTtsParam(
                this.parameters, textStream, preRequestId, false))
        .map(SpeechSynthesisResult::fromDashScopeResult)
        .filter(item -> !canceled.get())
        .doOnNext(
            result -> {
              if (result.getAudioFrame() != null) {
                if (recvAudioLength == 0) {
                  firstPackageTimeStamp = System.currentTimeMillis();
                  log.debug("[TtsV2] first package delay: " + getFirstPackageDelay() + " ms");
                }
                recvAudioLength +=
                    (double) result.getAudioFrame().capacity()
                        / ((double) (2 * parameters.getFormat().getSampleRate()) / 1000);
                long current = System.currentTimeMillis();
                double currentRtf = (current - startStreamTimeStamp) / recvAudioLength;
                log.debug(
                    "[TtsV2] Recv Audio Binary: "
                        + result.getAudioFrame().capacity()
                        + " bytes, total audio "
                        + recvAudioLength
                        + " ms, current_rtf: "
                        + currentRtf);
              }
            });
  }

  /**
   * Stream output speech synthesis using Flowable features (non-streaming input)
   *
   * @param text Text to be synthesized
   * @return The output event stream, including real-time audio and timestamps
   */
  public Flowable<SpeechSynthesisResult> callAsFlowable(String text)
      throws ApiException, NoApiKeyException {
    startStreamTimeStamp = System.currentTimeMillis();
    recvAudioLength = 0;
    preRequestId = UUID.randomUUID().toString();
    return duplexApi
        .duplexCall(
            StreamInputTtsParamWithStream.fromStreamInputTtsParam(
                this.parameters,
                Flowable.create(
                    emitter -> {
                      new Thread(
                              () -> {
                                emitter.onNext(text);
                                emitter.onComplete();
                              })
                          .start();
                    },
                    BackpressureStrategy.BUFFER),
                preRequestId,
                    true))
        .map(SpeechSynthesisResult::fromDashScopeResult)
        .doOnNext(
            result -> {
              if (result.getAudioFrame() != null) {
                if (recvAudioLength == 0) {
                  firstPackageTimeStamp = System.currentTimeMillis();
                  log.debug("[TtsV2] first package delay: " + getFirstPackageDelay() + " ms");
                }
                recvAudioLength +=
                    (double) result.getAudioFrame().capacity()
                        / ((double) (2 * parameters.getFormat().getSampleRate()) / 1000);
                long current = System.currentTimeMillis();
                double currentRtf = (current - startStreamTimeStamp) / recvAudioLength;
                log.debug(
                    "[TtsV2] Recv Audio Binary: "
                        + result.getAudioFrame().capacity()
                        + " bytes, total audio "
                        + recvAudioLength
                        + " ms, current_rtf: "
                        + currentRtf);
              }
            });
  }

  /**
   * Start voice transcription: Establish a connection with the server, send a voice transcription
   * request, and synchronously receive confirmation from the server.
   */
  private void startStream(boolean enableSsml) {

    startStreamTimeStamp = System.currentTimeMillis();
    recvAudioLength = 0;
    this.canceled.set(false);
    if (this.callback == null) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: ResultCallback is null"));
    }
    // 新的session开始，重置所有buffer
    outputStream = new ByteArrayOutputStream();
    audioData = null;
    //        timestamps.clear();
    WritableByteChannel channel = Channels.newChannel(outputStream);

    Flowable<String> textFrames =
        Flowable.create(
            emitter -> {
              synchronized (SpeechSynthesizer.this) {
                if (cmdBuffer.size() > 0) {
                  for (SpeechSynthesizer.AsyncCmdBuffer buffer : cmdBuffer) {
                    if (buffer.isStop) {
                      emitter.onComplete();
                      return;
                    } else {
                      emitter.onNext(buffer.text);
                    }
                  }
                  cmdBuffer.clear();
                }
                log.debug("set textEmitter");
                textEmitter = emitter;
              }
            },
            BackpressureStrategy.BUFFER);
    synchronized (this) {
      state = SpeechSynthesisState.TTS_STARTED;
      cmdBuffer.clear();
    }
    stopLatch = new AtomicReference<>(new CountDownLatch(1));
    preRequestId = UUID.randomUUID().toString();
    try {
      duplexApi.duplexCall(
          SpeechSynthesizer.StreamInputTtsParamWithStream.fromStreamInputTtsParam(
              this.parameters, textFrames, preRequestId, enableSsml),
          new ResultCallback<DashScopeResult>() {
            //                        private Sentence lastSentence = null;

            @Override
            public void onEvent(DashScopeResult message) {
              if (canceled.get()) {
                return;
              }
              SpeechSynthesisResult speechSynthesisResult =
                  SpeechSynthesisResult.fromDashScopeResult(message);

              try {
                /*
                if (speechSynthesisResult.getTimestamp() != null && !async_call) {
                    Sentence sentence = speechSynthesisResult.getTimestamp();
                    if (lastSentence == null) {
                        lastSentence = sentence;
                        if (sentence.getEndTime() != 0) {
                            timestamps.add(sentence);
                        }
                    } else {
                        if (!lastSentence.equals(sentence) && sentence.getEndTime() != 0) {
                            lastSentence = sentence;
                            timestamps.add(sentence);
                        }
                    }
                }
                 */
                if (speechSynthesisResult.getAudioFrame() != null) {
                  if (recvAudioLength == 0) {
                    firstPackageTimeStamp = System.currentTimeMillis();
                    log.debug("[TtsV2] first package delay: " + getFirstPackageDelay() + " ms");
                  }
                  recvAudioLength +=
                      (double) speechSynthesisResult.getAudioFrame().capacity()
                          / ((double) (2 * parameters.getFormat().getSampleRate()) / 1000);
                  long current = System.currentTimeMillis();
                  double currentRtf = (current - startStreamTimeStamp) / recvAudioLength;
                  log.debug(
                      "[TtsV2] Recv Audio Binary: "
                          + speechSynthesisResult.getAudioFrame().capacity()
                          + " bytes, total audio "
                          + recvAudioLength
                          + " ms, current_rtf: "
                          + currentRtf);
                  if (!asyncCall) {
                    try {
                      channel.write(speechSynthesisResult.getAudioFrame());
                    } catch (IOException e) {
                      log.error(
                          "Failed to write audio: {}", speechSynthesisResult.getAudioFrame(), e);
                    }
                  }
                }
              } catch (Exception e) {
                log.error("Failed to parse response: {}", message, e);
                callback.onError(e);
              }
              if (speechSynthesisResult.getRequestId() == null) {
                speechSynthesisResult.setRequestId(preRequestId);
              }
              callback.onEvent(speechSynthesisResult);
            }

            @Override
            public void onComplete() {
              log.debug("[TtsV2] onComplete");
              if (canceled.get()) {
                return;
              }
              synchronized (SpeechSynthesizer.this) {
                state = SpeechSynthesisState.IDLE;
              }
              audioData = ByteBuffer.wrap(outputStream.toByteArray());
              try {
                outputStream.close();
              } catch (IOException e) {
                log.error("Failed to close channel: {}", e);
              }
              callback.onComplete();
              if (stopLatch.get() != null) {
                stopLatch.get().countDown();
              }
            }

            @Override
            public void onError(Exception e) {
              if (canceled.get()) {
                return;
              }
              synchronized (SpeechSynthesizer.this) {
                state = SpeechSynthesisState.IDLE;
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
  }

  /**
   * Send text in a streaming manner
   *
   * @param text utf-8 encoded text
   */
  private void submitText(String text) {
    if (Objects.equals(text, "")) {
      throw new ApiException(new InputRequiredException("Parameter invalid: text is null"));
    }
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        throw new ApiException(
            new InputRequiredException(
                "State invalid: expect stream input tts state is started but " + state.getValue()));
      }
      if (textEmitter == null) {
        log.debug("submitText to new emitter: " + text);
        cmdBuffer.add(AsyncCmdBuffer.builder().text(text).build());
      } else {
        log.debug("submitText to emitter: " + text);
        textEmitter.onNext(text);
      }
    }
  }

  /**
   * Synchronously stop the streaming input speech synthesis task. Wait for all remaining
   * synthesized audio before returning
   *
   * @param completeTimeoutMillis The timeout period for await. If the timeout is set to a value
   *     greater than zero, it will wait for the corresponding number of milliseconds; otherwise, it
   *     will wait indefinitely. Throws TimeoutError exception if it times out.
   */
  public void streamingComplete(long completeTimeoutMillis) throws RuntimeException {
    log.debug("streamingComplete with timeout: " + completeTimeoutMillis);
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        throw new ApiException(
            new RuntimeException(
                "State invalid: expect stream input tts state is started but " + state.getValue()));
      }
      if (textEmitter == null) {
        log.debug("adding stop to new emitter");
        cmdBuffer.add(AsyncCmdBuffer.builder().isStop(true).build());
      } else {
        log.debug("adding stop to emitter");
        textEmitter.onComplete();
      }
    }

    if (stopLatch.get() != null) {
      try {
        if (completeTimeoutMillis > 0) {
          log.debug("start waiting for stopLatch");
          if (!stopLatch.get().await(completeTimeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("TimeoutError: waiting for streaming complete");
          }
        } else {
          log.debug("start waiting for stopLatch");
          stopLatch.get().await();
        }
        log.debug("stopLatch is done");
      } catch (InterruptedException ignored) {
        log.error("Interrupted while waiting for streaming complete");
      }
    }
  }

  /**
   * Synchronously stop the streaming input speech synthesis task. Wait for all remaining
   * synthesized audio before returning. If it does not complete within 600 seconds, a timeout
   * occurs and a TimeoutError exception is thrown.
   */
  public void streamingComplete() throws RuntimeException {
    streamingComplete(600000);
  }

  /**
   * Asynchronously stop the streaming input speech synthesis task, returns immediately. You need to
   * listen and handle the STREAM_INPUT_TTS_EVENT_SYNTHESIS_COMPLETE event in the on_event callback.
   * Do not destroy the object and callback before this event.
   */
  public void asyncStreamingComplete() {
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        throw new ApiException(
            new RuntimeException(
                "State invalid: expect stream input tts state is started but " + state.getValue()));
      }
      if (textEmitter == null) {
        cmdBuffer.add(AsyncCmdBuffer.builder().isStop(true).build());
      } else {
        textEmitter.onComplete();
      }
    }
  }

  /**
   * Immediately terminate the streaming input speech synthesis task and discard any remaining audio
   * that is not yet delivered.
   */
  public void streamingCancel() {
    canceled.set(true);
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        return;
      }
      if (textEmitter == null) {
        cmdBuffer.add(AsyncCmdBuffer.builder().isStop(true).build());
      } else {
        textEmitter.onComplete();
      }
    }
  }

  /**
   * Streaming input mode: You can call the stream_call function multiple times to send text. A
   * session will be created on the first call. The session ends after calling streaming_complete.
   *
   * @param text utf-8 encoded text
   */
  public void streamingCall(String text) {
    if (isFirst) {
      isFirst = false;
      this.startStream(false);
    }
    this.submitText(text);
  }

  /**
   * Speech synthesis If a callback is set, the audio will be returned in real-time through the
   * on_event interface Otherwise, this function blocks until all audio is received and then returns
   * the complete audio data.
   *
   * @param text utf-8 encoded text
   * @param timeoutMillis timeout for waiting audio data. If the timeout is set to a value greater
   *     than zero, it will wait for the corresponding number of milliseconds; otherwise, it will
   *     wait indefinitely.
   * @return If a callback is not set during initialization, the complete audio is returned as the
   *     function's return value. Otherwise, the return value is null.
   */
  public ByteBuffer call(String text, long timeoutMillis) throws RuntimeException {
    if (this.callback == null) {
      this.callback =
          new ResultCallback<SpeechSynthesisResult>() {
            @Override
            public void onEvent(SpeechSynthesisResult message) {}

            @Override
            public void onComplete() {}

            @Override
            public void onError(Exception e) {}
          };
    }
    this.startStream(true);
    this.submitText(text);
    if (this.asyncCall) {
      this.asyncStreamingComplete();
      return null;
    } else {
      this.streamingComplete(timeoutMillis);
      return audioData;
    }
  }

  /**
   * Speech synthesis If a callback is set, the audio will be returned in real-time through the
   * on_event interface Otherwise, this function blocks until all audio is received and then returns
   * the complete audio data.
   *
   * @param text utf-8 encoded text
   * @return If a callback is not set during initialization, the complete audio is returned as the
   *     function's return value. Otherwise, the return value is null.
   */
  public ByteBuffer call(String text) throws RuntimeException {
    return call(text, 0);
  }

  @SuperBuilder
  private static class AsyncCmdBuffer {
    @Builder.Default private boolean isStop = false;
    private String text;
  }

  @SuperBuilder
  private static class StreamInputTtsParamWithStream extends SpeechSynthesisParam {

    @NonNull private Flowable<String> textStream;

    public static StreamInputTtsParamWithStream fromStreamInputTtsParam(
        SpeechSynthesisParam param, Flowable<String> textStream, String preRequestId, boolean enableSsml) {
      return StreamInputTtsParamWithStream.builder()
          .headers(param.getHeaders())
          .parameters(param.getParameters())
          .parameter("pre_task_id", preRequestId)
          .parameter("enable_ssml", enableSsml)
          .format(param.getFormat())
          .textStream(textStream)
          .model(param.getModel())
          .voice(param.getVoice())
          .apiKey(param.getApiKey())
          .build();
    }

    @Override
    public Flowable<Object> getStreamingData() {
      return textStream
          .map(
              text -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("text", text);
                return jsonObject;
              })
          .cast(Object.class);
    }
  }

  /** First Package Delay is the time between start sending text and receive first audio package */
  public long getFirstPackageDelay() {
    return this.firstPackageTimeStamp - this.startStreamTimeStamp;
  }
}
