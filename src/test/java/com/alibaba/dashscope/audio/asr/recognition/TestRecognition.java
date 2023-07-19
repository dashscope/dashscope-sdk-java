package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Status;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestRecognition {
  private static final String TEST_FILE = "asr_example.pcm";

  public void downloadTestWav() {
    String testWavUrl =
        "https://isv-data.oss-cn-hangzhou.aliyuncs.com/ics/MaaS/ASR/test_audio/asr_example.pcm";
    try {
      InputStream in = new URL(testWavUrl).openStream();
      Files.copy(in, Paths.get(TEST_FILE), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      log.error("error: ", e);
    }
  }

  @BeforeEach
  public void setUp() {
    downloadTestWav();
  }

  @AfterEach
  public void tearDown() {
    try {
      Files.deleteIfExists(Paths.get(TEST_FILE));
    } catch (IOException e) {
      log.error("error: ", e);
    }
  }

  private Flowable<ByteBuffer> streamFromFile(String path, AtomicBoolean cancel) {
    return Flowable.create(
        emitter -> {
          new Thread(
                  () -> {
                    try {
                      log.info("start stream...");
                      File file = new File(path);
                      val channel = new FileInputStream(file).getChannel();
                      ByteBuffer buffer = ByteBuffer.allocate(4096);
                      while (channel.read(buffer) != -1 && !cancel.get()) {
                        buffer.flip();
                        emitter.onNext(buffer);
                        buffer = ByteBuffer.allocate(4096);
                        Thread.sleep(100);
                      }
                      log.info("complete with: {}", cancel.get() ? "canceled" : "done");
                      emitter.onComplete();
                    } catch (Exception e) {
                      log.error("stream error: {}", e.toString());
                      emitter.onError(e);
                    }
                  })
              .start();
        },
        BackpressureStrategy.BUFFER);
  }

  @Test
  public void testBasicRecognition() {
    log.info("testBasicRecognition...");
    assert (Files.exists(Paths.get(TEST_FILE)));
    AtomicBoolean cancel = new AtomicBoolean(false);
    Recognition recognizer = new Recognition();
    RecognitionParam param =
        RecognitionParam.builder()
            .model("paraformer-realtime-v1")
            .format("pcm")
            .sampleRate(16000)
            .apiKey(System.getenv("TEST_API_KEY"))
            .build();
    try {
      recognizer
          .streamCall(param, streamFromFile(TEST_FILE, cancel))
          .blockingSubscribe(
              result -> {
                log.info("result: {}", result);
              },
              error -> {
                log.error("error: {}", error.toString());
                cancel.set(true);
              },
              () -> {
                log.info("complete");
              });
    } catch (Exception e) {
      log.error("out error: {}", e.toString());
    }
    log.info("testBasicRecognition...done");
  }

  @Test
  public void testCallbackRecognition() {
    log.info("testCallbackRecognition...");
    assert (Files.exists(Paths.get(TEST_FILE)));
    Recognition recognizer = new Recognition();
    AtomicBoolean cancel = new AtomicBoolean(false);
    RecognitionParam param =
        RecognitionParam.builder()
            .model("paraformer-realtime-v1")
            .format("pcm")
            .sampleRate(16000)
            .apiKey(System.getenv("TEST_API_KEY"))
            .build();

    class RecognitionCallback extends ResultCallback<RecognitionResult> {

      @Override
      public void onEvent(RecognitionResult message) {
        log.info("onEvent: {}", message);
      }

      @Override
      public void onOpen(Status status) {
        log.info("onOpen: {}", status);
      }

      @Override
      public void onComplete() {
        log.info("onComplete!!!");
      }

      @Override
      public void onError(Exception e) {
        log.error("onError: {}", e.toString());
        cancel.set(true);
      }
    }

    try {
      recognizer.call(param, new RecognitionCallback());
      File file = new File(TEST_FILE);
      val channel = new FileInputStream(file).getChannel();
      ByteBuffer buffer = ByteBuffer.allocate(4096);
      while (channel.read(buffer) != -1 && !cancel.get()) {
        log.info("send audio...");
        buffer.flip();
        recognizer.sendAudioFrame(buffer);
        buffer = ByteBuffer.allocate(4096);
        // Thread.sleep(50);
      }
      log.info("send complete");
      recognizer.stop();
    } catch (Exception e) {
      log.error("out error: {}", e.toString());
    }
  }

  @Test
  public void testSyncRecognition() {
    log.info("testSyncRecognition...");
    assert (Files.exists(Paths.get(TEST_FILE)));
    Recognition recognizer = new Recognition();
    RecognitionParam param =
        RecognitionParam.builder()
            .model("paraformer-realtime-v1")
            .format("pcm")
            .sampleRate(16000)
            .apiKey(System.getenv("TEST_API_KEY"))
            .build();

    String result = recognizer.call(param, new File(TEST_FILE));
    log.info(result);
    log.info("testSyncRecognition...done");
  }
}
