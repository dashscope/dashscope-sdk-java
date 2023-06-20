package com.alibaba.dashscope.audio.tts;

import com.alibaba.dashscope.audio.tts.timestamp.Sentence;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Request;
import com.alibaba.dashscope.protocol.Response;
import com.alibaba.dashscope.protocol.ServiceFacility;
import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SpeechSynthesizer implements BaseSpeechSynthesizer {

  @Setter private long responseTimeout = 30;

  @Getter private final List<Sentence> timestamps = Lists.newCopyOnWriteArrayList();

  @Getter private ByteBuffer audioData;

  private final Protocol protocol = Protocol.WEBSOCKET;

  public SpeechSynthesizer() {}

  @Override
  public void call(SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback) {
    timestamps.clear();
    audioData = null;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WritableByteChannel channel = Channels.newChannel(outputStream);
    class SynthesisCallback extends ResultCallback<Response> {
      private Sentence lastSentence = null;

      public void onOpen(Status status) {
        callback.onOpen(status);
      }

      @Override
      public void onEvent(Response message) {
        SpeechSynthesisResult result = null;
        try {
          result = (SpeechSynthesisResult) ServiceFacility.prepareResult(protocol, param, message);
          if (result.getTimestamp() != null) {
            Sentence sentence = result.getTimestamp();
            if (lastSentence == null) {
              lastSentence = sentence;
              timestamps.add(sentence);
            } else {
              if (!lastSentence.equals(sentence)) {
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

      public void onClose(Status status) {
        callback.onClose(status);
      }

      @Override
      public void onError(Exception e) {
        callback.onError(e);
      }

      public void doClose(Status status) {
        callback.doClose(status);
      }
    }

    param.setMode(StreamingMode.OUT);
    String url = ServiceFacility.prepareUrl(protocol, param);
    Map<String, String> header = ServiceFacility.prepareHeaders(protocol, param);
    Request request = ServiceFacility.prepareRequest(protocol, param, WebSocketEventType.RUN_TASK);
    ServiceFacility.streamingOutWithCallback(
        protocol, url, header, request, HttpMethod.POST, param.getMode(), new SynthesisCallback());
  }

  @Override
  public Flowable<SpeechSynthesisResult> streamCall(SpeechSynthesisParam param) {
    param.setMode(StreamingMode.OUT);
    audioData = null;
    timestamps.clear();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    WritableByteChannel channel = Channels.newChannel(outputStream);
    AtomicReference<Sentence> lastSentenceRef = new AtomicReference<>();
    return ServiceFacility.streamCall(protocol, null, param, responseTimeout)
        .map(
            message -> {
              SpeechSynthesisResult result = (SpeechSynthesisResult) message;
              if (result.getTimestamp() != null) {
                Sentence sentence = result.getTimestamp();
                if (lastSentenceRef.get() == null) {
                  lastSentenceRef.set(sentence);
                  timestamps.add(sentence);
                } else {
                  if (!lastSentenceRef.get().equals(sentence)) {
                    lastSentenceRef.set(sentence);
                    timestamps.add(sentence);
                  }
                }
                timestamps.add(result.getTimestamp());
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
              audioData = ByteBuffer.wrap(outputStream.toByteArray());
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
  }

  @Override
  public ByteBuffer call(SpeechSynthesisParam param) {
    streamCall(param).blockingSubscribe();
    return audioData;
  }
}
