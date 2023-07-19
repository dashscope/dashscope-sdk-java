package com.alibaba.dashscope.audio.asr.transcription;

import static java.lang.Thread.sleep;

import com.alibaba.dashscope.common.TaskStatus;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestTranscription {

  @Test
  public void testBasicTranscription() {
    log.info("testBasicTranscription...");

    TranscriptionParam param =
        TranscriptionParam.builder()
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .model("paraformer-v1")
            .fileUrls(
                Collections.singletonList(
                    "https://isv-data.oss-cn-hangzhou.aliyuncs.com/ics/MaaS/ASR/test_audio/asr_example.wav"))
            .build();
    try {
      Transcription transcription = new Transcription();
      TranscriptionResult result = transcription.asyncCall(param);
      log.info("submit task result: {}", result);
      result =
          transcription.wait(
              TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
      log.info("result: {}", result);
    } catch (Exception e) {
      log.error("error: {}", e);
    }
  }

  @Test
  public void testSubmitFetchTranscription() {
    log.info("testBasicTranscription...");

    TranscriptionParam param =
        TranscriptionParam.builder()
            .apiKey(System.getenv().get("TEST_API_KEY"))
            .model("paraformer-v1")
            .fileUrls(
                Collections.singletonList(
                    "https://isv-data.oss-cn-hangzhou.aliyuncs.com/ics/MaaS/ASR/test_audio/asr_example.wav"))
            .build();
    Transcription transcription = new Transcription();
    TranscriptionResult result = null;
    try {
      result = transcription.asyncCall(param);
    } catch (Exception e) {
      log.error("error: {}", e);
      throw new RuntimeException(e);
    }
    while (true) {
      try {
        result =
            transcription.fetch(
                TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
        log.info("result: {}", result);
        if (result.getTaskStatus() == TaskStatus.SUCCEEDED
            || result.getTaskStatus() == TaskStatus.FAILED
            || result.getTaskStatus() == TaskStatus.CANCELED
            || result.getTaskStatus() == TaskStatus.UNKNOWN) {
          break;
        }
      } catch (Exception e) {
        log.error("error: {}", e);
      } finally {
        try {
          sleep(500);
        } catch (InterruptedException ignored) {

        }
      }
    }
    assert (result.getMetrics().getSucceeded() > 0);
  }
}
