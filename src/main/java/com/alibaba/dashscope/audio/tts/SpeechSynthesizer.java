// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.tts;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.audio.tts.timestamp.Sentence;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SpeechSynthesizer {

  @Getter
  private final List<Sentence> timestamps = new ArrayList<>(); // Lists.newCopyOnWriteArrayList();

  @Getter private ByteBuffer audioData;

  private AtomicReference<String> lastRequestId = new AtomicReference<>();

  public String getLastRequestId() {
    return lastRequestId.get();
  }

  private ApiServiceOption serviceOption =
      ApiServiceOption.builder()
          .protocol(Protocol.WEBSOCKET)
          .streamingMode(StreamingMode.OUT)
          .outputMode(OutputMode.ACCUMULATE)
          .taskGroup(TaskGroup.AUDIO.getValue())
          .task(Task.TEXT_TO_SPEECH.getValue())
          .function(Function.SPEECH_SYNTHESIZER.getValue())
          .build();
  @Getter private final SynchronizeHalfDuplexApi<SpeechSynthesisParam> syncApi;

  public SpeechSynthesizer() {
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public void call(SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback) {
    timestamps.clear();
    audioData = null;
    lastRequestId.set(null);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WritableByteChannel channel = Channels.newChannel(outputStream);

    class SynthesisCallback extends ResultCallback<DashScopeResult> {
      private Sentence lastSentence = null;

      public void onOpen(Status status) {
        callback.onOpen(status);
      }

      @Override
      public void onEvent(DashScopeResult message) {
        SpeechSynthesisResult result = SpeechSynthesisResult.fromDashScopeResult(message);
        try {
          if (lastRequestId.get() == null) {
            lastRequestId.set(result.getRequestId());
          }
          if (result.getTimestamp() != null) {
            Sentence sentence = result.getTimestamp();
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
          if (result.getAudioFrame() != null) {
            try {
              channel.write(result.getAudioFrame());
            } catch (IOException e) {
              log.error("Failed to write audio: {}", result.getAudioFrame(), e);
            }
          }
        } catch (Exception e) {
          log.error("Failed to parse response: {}", message, e);
          callback.onError(e);
        }
        callback.onEvent(result);
      }

      @Override
      public void onComplete() {
        try {
          audioData = ByteBuffer.wrap(outputStream.toByteArray());
          channel.close();
          outputStream.close();
        } catch (IOException e) {
          log.error("Failed to close channel: {}", channel, e);
        }
        callback.onComplete();
      }

      @Override
      public void onError(Exception e) {
        callback.onError(e);
      }
    }
    try {
      syncApi.streamCall(param, new SynthesisCallback());
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }

  public Flowable<SpeechSynthesisResult> streamCall(SpeechSynthesisParam param) {
    audioData = null;
    timestamps.clear();
    lastRequestId.set(null);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WritableByteChannel channel = Channels.newChannel(outputStream);
    AtomicReference<Sentence> lastSentenceRef = new AtomicReference<>();
    try {
      return syncApi
          .streamCall(param)
          .map(
              message -> {
                SpeechSynthesisResult result = SpeechSynthesisResult.fromDashScopeResult(message);
                if (lastRequestId.get() == null) {
                  lastRequestId.set(result.getRequestId());
                }
                if (result.getTimestamp() != null) {
                  Sentence sentence = result.getTimestamp();
                  if (lastSentenceRef.get() == null) {
                    lastSentenceRef.set(sentence);
                    if (sentence.getEndTime() != 0) {
                      timestamps.add(sentence);
                    }
                  } else {
                    if (!lastSentenceRef.get().equals(sentence) && sentence.getEndTime() != 0) {
                      lastSentenceRef.set(sentence);
                      timestamps.add(sentence);
                    }
                  }
                }
                if (result.getAudioFrame() != null) {
                  try {
                    channel.write(result.getAudioFrame());
                  } catch (IOException e) {
                    log.error("Failed to write audio: {}", result.getAudioFrame(), e);
                  }
                }
                return result;
              })
          .doOnComplete(
              () -> {
                try {
                  audioData = ByteBuffer.wrap(outputStream.toByteArray());
                  channel.close();
                  outputStream.close();
                } catch (IOException e) {
                  log.error("Failed to close channel: {}", channel, e);
                }
              })
          .doOnError(
              e -> {
                try {
                  channel.close();
                  outputStream.close();
                  timestamps.clear();
                  audioData = null;
                } catch (IOException ex) {
                  log.error("Failed to close channel: {}", channel, ex);
                }
              });
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }

  public ByteBuffer call(SpeechSynthesisParam param) {
    AtomicReference<Throwable> finalError = new AtomicReference<>(null);
    Flowable<SpeechSynthesisResult> flowable = streamCall(param);
    flowable.doOnError(finalError::set).blockingLast();
    if (finalError.get() != null) {
      throw new ApiException(finalError.get());
    }
    return audioData;
  }
}
