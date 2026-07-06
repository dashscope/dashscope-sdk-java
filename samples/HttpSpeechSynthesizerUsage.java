// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.audio.http_tts.AudioInfo;
import com.alibaba.dashscope.audio.http_tts.HttpSpeechSynthesisParam;
import com.alibaba.dashscope.audio.http_tts.HttpSpeechSynthesisResult;
import com.alibaba.dashscope.audio.http_tts.HttpSpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

/**
 * Example usage of HttpSpeechSynthesizer for HTTP SSE-based text-to-speech synthesis.
 *
 * <p>Make sure to set the DASHSCOPE_API_KEY environment variable before running this example.
 *
 * @author DashScope SDK Team
 */
public class HttpSpeechSynthesizerUsage {

  /**
   * Demonstrates synchronous call with SSE - blocks until synthesis is complete and returns audio
   * data.
   */
  public static void syncCall() {
    System.out.println("=== Synchronous Call with SSE Example ===");

    // Create synthesizer
    HttpSpeechSynthesizer synthesizer = new HttpSpeechSynthesizer();

    // Build parameters
    HttpSpeechSynthesisParam param =
        HttpSpeechSynthesisParam.builder()
            .model("cosyvoice-v3-flash")
            .text("我家的后面有一个很大的园。")
            .voice("longanyang")
            .format("wav")
            .sampleRate(24000)
            .build();

    try {
      // Call and get complete audio data
      ByteBuffer audioData = synthesizer.callAndReturnAudio(param);

      // Save to file
      if (audioData != null && audioData.hasRemaining()) {
        byte[] bytes = new byte[audioData.remaining()];
        audioData.get(bytes);

        try (FileOutputStream fos = new FileOutputStream("sync_output.wav")) {
          fos.write(bytes);
          System.out.println("Audio saved to sync_output.wav, size: " + bytes.length + " bytes");
        } catch (IOException e) {
          System.err.println("Failed to save audio: " + e.getMessage());
        }
      }
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("Synthesis failed: " + e.getMessage());
    }
  }

  /**
   * Demonstrates synchronous call without SSE - returns audio URL instead of audio data. This is a
   * simpler and faster way to get the synthesized audio.
   */
  public static void syncCallWithUrl() {
    System.out.println("\n=== Synchronous Call without SSE (returns Audio URL) ===");

    HttpSpeechSynthesizer synthesizer = new HttpSpeechSynthesizer();

    HttpSpeechSynthesisParam param =
        HttpSpeechSynthesisParam.builder()
            .model("cosyvoice-v3-flash")
            .text("我家的后面有一个很大的园。")
            .voice("longanyang")
            .format("wav")
            .sampleRate(24000)
            .build();

    try {
      // Non-SSE call - returns result with audio URL
      HttpSpeechSynthesisResult result = synthesizer.call(param);

      System.out.println("Request ID: " + result.getRequestId());
      System.out.println("Finish Reason: " + result.getFinishReason());

      if (result.hasAudioUrl()) {
        AudioInfo audioInfo = result.getAudioInfo();
        System.out.println("\nAudio URL: " + audioInfo.getUrl());
        System.out.println("Audio ID: " + audioInfo.getId());
        System.out.println("Expires At: " + audioInfo.getExpiresAt());
        System.out.println("Remaining Time: " + audioInfo.getRemainingSeconds() + " seconds");
        System.out.println("URL Expired: " + audioInfo.isExpired());

        // You can download the audio from the URL
        // Example: use HttpURLConnection or any HTTP client to download
        System.out.println("\nTip: You can download the audio file from the URL above.");
      }

    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("Synthesis failed: " + e.getMessage());
    }
  }

  /** Demonstrates streaming call with callback - receives audio chunks as they arrive. */
  public static void streamCallWithCallback() {
    System.out.println("\n=== Streaming Call with Callback Example ===");

    HttpSpeechSynthesizer synthesizer = new HttpSpeechSynthesizer();

    HttpSpeechSynthesisParam param =
        HttpSpeechSynthesisParam.builder()
            .model("cosyvoice-v3-flash")
            .text("今天天气真好，适合出去玩。")
            .voice("longanyang")
            .format("wav")
            .sampleRate(24000)
            .build();

    // Use CountDownLatch to wait for completion
    CountDownLatch latch = new CountDownLatch(1);

    try {
      synthesizer.streamCall(
          param,
          new ResultCallback<HttpSpeechSynthesisResult>() {
            private int chunkCount = 0;

            @Override
            public void onEvent(HttpSpeechSynthesisResult result) {
              chunkCount++;
              if (result.hasAudioData()) {
                System.out.println(
                    "Received chunk #"
                        + chunkCount
                        + ", size: "
                        + result.getAudioDataSize()
                        + " bytes");
              }
              if (result.getRequestId() != null) {
                System.out.println("Request ID: " + result.getRequestId());
              }
            }

            @Override
            public void onComplete() {
              latch.countDown();
            }

            @Override
            public void onError(Exception e) {
              System.err.println("✗ Error during synthesis: " + e.getMessage());
              latch.countDown();
            }
          });

      // Wait for completion
      latch.await();
      System.out.println("Done!");

    } catch (ApiException | NoApiKeyException | InputRequiredException | InterruptedException e) {
      System.err.println("Failed: " + e.getMessage());
    }
  }

  /** Demonstrates custom parameter settings. */
  public static void customParameters() {
    System.out.println("\n=== Custom Parameters Example ===");

    HttpSpeechSynthesizer synthesizer = new HttpSpeechSynthesizer();

    // Build parameters with custom voice settings
    HttpSpeechSynthesisParam param =
        HttpSpeechSynthesisParam.builder()
            .model("cosyvoice-v3-flash")
            .text("这是一段测试语音合成参数的文本。")
            .voice("longanyang")
            .format("wav")
            .sampleRate(24000)
            .volume(80) // Volume: 0-100
            .rate(1.2f) // Speech rate: 0.5-2.0
            .pitch(1.1f) // Pitch: 0.5-2.0
            .build();

    System.out.println("Parameters:");
    System.out.println("  Model: " + param.getModel());
    System.out.println("  Text: " + param.getText());
    System.out.println("  Voice: " + param.getVoice());
    System.out.println("  Format: " + param.getFormat());
    System.out.println("  Sample Rate: " + param.getSampleRate());
    System.out.println("  Volume: " + param.getVolume());
    System.out.println("  Rate: " + param.getRate());
    System.out.println("  Pitch: " + param.getPitch());

    try {
      ByteBuffer audioData = synthesizer.callAndReturnAudio(param);
      if (audioData != null) {
        System.out.println(
            "✓ Synthesis completed, audio size: " + audioData.remaining() + " bytes");
      }
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("Failed: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    Constants.apiKey = System.getenv("DASHSCOPE_API_KEY");
    System.out.println("HttpSpeechSynthesizer Usage Examples\n");
    System.out.println("====================================\n");

    // Run examples
    syncCall(); // SSE streaming - returns audio data
    syncCallWithUrl(); // Non-SSE - returns audio URL
    streamCallWithCallback();
    customParameters();

    System.out.println("\n====================================");
    System.out.println("All examples completed!");
  }
}
