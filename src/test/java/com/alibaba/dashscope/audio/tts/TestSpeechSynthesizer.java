package com.alibaba.dashscope.audio.tts;

import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestSpeechSynthesizer {
  private static final String SHORT_TEXT = "今天天气。明天天气。";

  @Test
  public void testBasicSynthesis() throws InterruptedException {
    log.info("testBasicSynthesis...");
    SpeechSynthesizer synthesis = new SpeechSynthesizer();
    String voice = "sambert-zhimao-v1";

    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .text(SHORT_TEXT)
            .model(voice)
            .enableWordTimestamp(true)
            .enablePhonemeTimestamp(true)
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .build();

    log.info("model: {}, param: {}", voice, param);
    CountDownLatch latch = new CountDownLatch(1);
    File file = new File("test.wav");
    long time0 = System.currentTimeMillis();
    class ReactCallback extends ResultCallback<SpeechSynthesisResult> {
      private boolean firstPackage = true;
      private long totalSize = 0;

      @Override
      public void onOpen(Status status) {
        log.info("onOpen status: {}", status);
      }

      @Override
      public void onEvent(SpeechSynthesisResult result) {
        if (result.getUsage() != null) {
          log.info("usage: {}", JsonUtils.toJson(result.getUsage()));
        }
        if (result.getAudioFrame() != null) {
          log.info("audio result length: {}", result.getAudioFrame().array().length);
          totalSize += result.getAudioFrame().array().length;
          if (firstPackage) {
            firstPackage = false;
            log.info("first pack time: {}", System.currentTimeMillis() - time0);
          }
          // append write
          try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(result.getAudioFrame().array());
          } catch (IOException e) {
            log.error("write audio error", e);
          }
        }
        if (result.getTimestamp() != null) {
          log.info("timestamp result: {}", result.getTimestamp());
        }
      }

      @Override
      public void onComplete() {
        log.info("onComplete");
        long audioLengthInMs = (totalSize - 44) / 32;
        long time1 = System.currentTimeMillis() - time0;
        log.info("rtf: {}", (float) time1 / (float) audioLengthInMs);
        latch.countDown();
      }

      @Override
      public void onError(Exception e) {
        log.info("onError", e);
        latch.countDown();
      }
    }

    synthesis.call(param, new ReactCallback());
    latch.await();
    // get file size
    assert (file.length() > 0);
    assert (synthesis.getAudioData().array().length > 0);
    assert (synthesis.getTimestamps().size() > 0);
    // remove file
    file.delete();
  }

  @Test
  public void testStreamSynthesis() {
    log.info("testStreamSynthesis...");
    SpeechSynthesizer synthesis = new SpeechSynthesizer();
    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .model("sambert-zhimao-v1")
            .text(SHORT_TEXT)
            .enableWordTimestamp(true)
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .build();

    Flowable<SpeechSynthesisResult> flowable = synthesis.streamCall(param);
    flowable.blockingForEach(
        msg -> {
          if (msg.getAudioFrame() != null) {
            log.info("audio result length: {}", msg.getAudioFrame().array().length);
          }
          if (msg.getTimestamp() != null) {
            log.info("timestamp result: {}", msg.getTimestamp());
          }
        });

    assert (synthesis.getAudioData() != null);
    assert (synthesis.getTimestamps().size() > 0);
  }

  @Test
  public void testSyncCallSynthesis() {
    log.info("testSyncCallSynthesis...");
    SpeechSynthesizer synthesis = new SpeechSynthesizer();
    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .model("sambert-zhimao-v1")
            .text(SHORT_TEXT)
            .enableWordTimestamp(true)
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .build();

    synthesis.call(param);
    assert (synthesis.getAudioData() != null);
    assert (synthesis.getTimestamps() != null && synthesis.getTimestamps().size() > 0);
  }

  @Test
  public void testSyncCallSynthesisForNoTimestamp() {
    log.info("testSyncCallSynthesis...");
    SpeechSynthesizer synthesis = new SpeechSynthesizer();
    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .model("sambert-zhimao-v1")
            .text(SHORT_TEXT)
            .enableWordTimestamp(false)
            .enablePhonemeTimestamp(false)
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .build();

    log.info("model: zhimiao_emo, param: {}", param);

    synthesis.call(param);
    assert (synthesis.getAudioData() != null);
    // assert (synthesis.getTimestamps() != null && synthesis.getTimestamps().size() > 0);
  }

  @Test
  public void testSyncCallSynthesisForNoWordTimestamp() {
    log.info("testSyncCallSynthesisForNoWordTimestamp...");
    SpeechSynthesizer synthesis = new SpeechSynthesizer();
    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .model("sambert-zhimao-v1")
            .text(SHORT_TEXT)
            .enableWordTimestamp(false)
            .enablePhonemeTimestamp(true)
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .build();

    log.info("model: zhimiao_emo, param: {}", param);

    synthesis.call(param);
    log.info("synthesis.getTimestamps(): {}", synthesis.getTimestamps());
    assert (synthesis.getAudioData() != null);
    assert (synthesis.getTimestamps() != null && synthesis.getTimestamps().size() == 0);
  }

  @Test
  public void testSyncCallSynthesisForNoPhonemeTimestamp() {
    log.info("testSyncCallSynthesisForNoPhonemeTimestamp...");
    SpeechSynthesizer synthesis = new SpeechSynthesizer();
    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .model("sambert-zhimao-v1")
            .text(SHORT_TEXT)
            .enableWordTimestamp(true)
            .enablePhonemeTimestamp(false)
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .build();

    log.info("model: zhimiao_emo, param: {}", param);

    synthesis.call(param);
    assert (synthesis.getAudioData() != null);
    assert (synthesis.getTimestamps() != null && synthesis.getTimestamps().size() > 0);
  }

  @Test
  public void testReuseSynthesis() {
    log.info("testResuseSynthesis...");
    SpeechSynthesizer synthesis = new SpeechSynthesizer();
    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .model("sambert-zhimao-v1")
            .text(SHORT_TEXT)
            .enableWordTimestamp(true)
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .build();

    for (int i = 0; i < 3; i++) {
      if (i == 0) {
        assert (synthesis.getAudioData() == null);
        assert (synthesis.getTimestamps().size() == 0);
      }

      Flowable<SpeechSynthesisResult> flowable = synthesis.streamCall(param);
      flowable.blockingForEach(
          msg -> {
            if (msg.getAudioFrame() != null) {
              log.info("audio result length: {}", msg.getAudioFrame().array().length);
            }
            if (msg.getTimestamp() != null) {
              log.info("timestamp result: {}", msg.getTimestamp());
            }
          });

      assert (synthesis.getAudioData() != null);
      assert (synthesis.getTimestamps().size() > 0);
    }
  }

  @Test
  public void testMultithreadRunSynthesis() {
    log.info("testMultithreadRunSynthesis...");
    List<Thread> threadList = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      threadList.add(
          new Thread(
              () -> {
                SpeechSynthesizer synthesis = new SpeechSynthesizer();
                SpeechSynthesisParam param =
                    SpeechSynthesisParam.builder()
                        .model("sambert-zhimao-v1")
                        .text(SHORT_TEXT)
                        .enableWordTimestamp(true)
                        .apiKey(System.getenv().get("TEST_API_KEY"))
                        .build();

                Flowable<SpeechSynthesisResult> flowable = synthesis.streamCall(param);
                flowable.blockingForEach(
                    msg -> {
                      if (msg.getAudioFrame() != null) {
                        log.info("audio result length: {}", msg.getAudioFrame().array().length);
                      }
                      if (msg.getTimestamp() != null) {
                        log.info("timestamp result: {}", msg.getTimestamp());
                      }
                    });

                assert (synthesis.getAudioData() != null);
                assert (synthesis.getTimestamps().size() > 0);
              }));
    }
    for (Thread thread : threadList) {
      thread.start();
    }
    for (Thread thread : threadList) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        log.error("thread join error", e);
      }
    }
  }
}
