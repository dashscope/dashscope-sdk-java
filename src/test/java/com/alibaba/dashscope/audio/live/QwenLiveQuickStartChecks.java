// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import com.alibaba.dashscope.utils.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public final class QwenLiveQuickStartChecks {
  public static void main(String[] args) throws Exception {
    try {
      savesExactAudioFragments();
      silenceWithoutAudioTimesOut();
      newAudioRestartsIdleWindow();
      earlyAudioStillWaitsAfterInputEnds();
      errorsUnblockWaitingAndPreserveTheirCause();
      fileWriteFailureIsNotReportedAsSuccess();
      sessionUpdateMustBeConfirmed();
      malformedAudioIsNotReportedAsSuccess();
      doesNotOverwriteOutputAndRejectsBadWaitSettings();
      fileToReplyLoopWaitsForDelayedAudio();
      standaloneCliExitsAfterSuccessAndFailure();
      invalidCliArgumentsReturnFailure();
      System.out.println("QUICK_START_CHECKS_OK: 15 cases");
    } finally {
      okhttp3.OkHttpClient client =
          com.alibaba.dashscope.protocol.okhttp.OkHttpClientFactory.getOkHttpClient();
      client.dispatcher().executorService().shutdownNow();
      client.connectionPool().evictAll();
    }
  }

  private static final class Scenario implements AutoCloseable {
    final Path directory = Files.createTempDirectory(Paths.get("target"), "quickstart-scenario-");
    final Path input = directory.resolve("input.pcm");
    final Path output = directory.resolve("reply.pcm");
    final MockWebServer server = new MockWebServer();
    final ScheduledExecutorService sender = Executors.newSingleThreadScheduledExecutor();
    final ByteArrayOutputStream received = new ByteArrayOutputStream();
    final AtomicLong finalInputAt = new AtomicLong();
    Consumer<WebSocket> update = socket -> socket.send("{\"type\":\"session.updated\"}");
    Consumer<WebSocket> firstInput = socket -> {};
    Consumer<WebSocket> finalInput = socket -> {};
    QwenLiveConnectionParam param;

    Scenario() throws Exception {
      Files.write(input, new byte[] {1, 2, -1, -2});
      server.enqueue(
          new MockResponse()
              .withWebSocketUpgrade(
                  new WebSocketListener() {
                    @Override
                    public void onMessage(WebSocket socket, String text) {
                      com.google.gson.JsonObject event = JsonUtils.parse(text);
                      String type = event.get("type").getAsString();
                      if ("session.update".equals(type)) update.accept(socket);
                      if ("session.input_audio.append".equals(type)) {
                        boolean first = received.size() == 0;
                        byte[] bytes = Base64.getDecoder().decode(event.get("audio").getAsString());
                        received.write(bytes, 0, bytes.length);
                        if (first) firstInput.accept(socket);
                        if (received.size() == 32004) {
                          finalInputAt.set(System.nanoTime());
                          finalInput.accept(socket);
                        }
                      }
                    }

                    @Override
                    public void onClosing(WebSocket socket, int code, String reason) {
                      socket.close(code, reason);
                    }
                  }));
      server.start();
      param =
          QwenLiveConnectionParam.builder()
              .model("qwen-live")
              .apikey("test-key")
              .url(server.url("/realtime").toString())
              .build();
    }

    void run(long idle, long timeout) throws Exception {
      runSample(param, input, output, idle, timeout);
    }

    @Override
    public void close() throws Exception {
      sender.shutdownNow();
      check(sender.awaitTermination(2, TimeUnit.SECONDS), "test sender did not stop");
      server.shutdown();
      Files.deleteIfExists(input);
      Files.deleteIfExists(output);
      Files.delete(directory);
    }
  }

  private static void audio(WebSocket socket, String field, String data) {
    socket.send("{\"type\":\"session.output_audio.delta\",\"" + field + "\":\"" + data + "\"}");
  }

  private static void savesExactAudioFragments() throws Exception {
    try (Scenario scenario = new Scenario()) {
      scenario.finalInput =
          socket -> {
            audio(socket, "audio", "AAEC");
            audio(socket, "delta", "A/8=");
          };
      scenario.run(50, 1000);
      check(
          Arrays.equals(new byte[] {0, 1, 2, 3, -1}, Files.readAllBytes(scenario.output)),
          "audio fragments must be saved exactly once");
    }
  }

  private static void silenceWithoutAudioTimesOut() throws Exception {
    try (Scenario scenario = new Scenario()) {
      scenario.finalInput =
          socket -> {
            audio(socket, "audio", "");
            socket.send("{\"type\":\"session.output_transcript.delta\",\"delta\":\"text only\"}");
          };
      try {
        scenario.run(20, 200);
        throw new AssertionError("empty audio/transcript must not count as completed output");
      } catch (TimeoutException expected) {
        check(Files.size(scenario.output) == 0, "empty audio wrote unexpected bytes");
      }
    }
  }

  private static void newAudioRestartsIdleWindow() throws Exception {
    try (Scenario scenario = new Scenario()) {
      AtomicLong secondAudioAt = new AtomicLong();
      scenario.finalInput =
          socket -> {
            audio(socket, "audio", "AQI=");
            scenario.sender.schedule(
                () -> {
                  secondAudioAt.set(System.nanoTime());
                  audio(socket, "audio", "AwQ=");
                },
                180,
                TimeUnit.MILLISECONDS);
          };
      scenario.run(250, 2000);
      check(secondAudioAt.get() != 0, "sample closed before later audio");
      check(
          System.nanoTime() - secondAudioAt.get() >= TimeUnit.MILLISECONDS.toNanos(230),
          "later audio must extend waiting");
      check(
          Arrays.equals(new byte[] {1, 2, 3, 4}, Files.readAllBytes(scenario.output)),
          "later audio was lost");
    }
  }

  private static void earlyAudioStillWaitsAfterInputEnds() throws Exception {
    try (Scenario scenario = new Scenario()) {
      scenario.firstInput = socket -> audio(socket, "audio", "AQI=");
      scenario.run(180, 2000);
      check(
          System.nanoTime() - scenario.finalInputAt.get() >= TimeUnit.MILLISECONDS.toNanos(250),
          "audio during input must not cause immediate closure at EOF");
    }
  }

  private static void errorsUnblockWaitingAndPreserveTheirCause() throws Exception {
    for (String error : new String[] {"server", "transport", "session"}) {
      try (Scenario scenario = new Scenario()) {
        scenario.finalInput =
            socket -> {
              if ("server".equals(error)) {
                socket.send(
                    "{\"type\":\"error\",\"error\":{\"code\":\"test_error\",\"message\":\"request-failed\"}}");
              } else if ("transport".equals(error)) {
                socket.close(1000, "unexpected close");
              } else {
                socket.send("{\"type\":\"session.closed\",\"reason\":\"unexpected end\"}");
              }
            };
        try {
          scenario.run(50, 2000);
          throw new AssertionError(error + " error cannot become successful output");
        } catch (IllegalStateException expected) {
          check(expected.getCause() != null, "original failure must be preserved");
          if ("server".equals(error)) {
            check(
                expected.getCause() instanceof com.alibaba.dashscope.exception.ApiException,
                "server failure must retain ApiException");
          } else {
            check(
                expected.getCause() instanceof IOException,
                "early closure must explain incomplete output");
          }
        }
      }
    }
  }

  private static void fileWriteFailureIsNotReportedAsSuccess() throws Exception {
    ExecutorService worker = Executors.newSingleThreadExecutor();
    AtomicBoolean interruptAudio = new AtomicBoolean();
    try (Scenario scenario = new Scenario()) {
      scenario.param.setCallbackExecutor(
          task ->
              worker.execute(
                  () -> {
                    if (interruptAudio.getAndSet(false)) Thread.currentThread().interrupt();
                    try {
                      task.run();
                    } finally {
                      Thread.interrupted();
                    }
                  }));
      scenario.finalInput =
          socket -> {
            interruptAudio.set(true);
            audio(socket, "audio", Base64.getEncoder().encodeToString(new byte[10000]));
          };
      try {
        scenario.run(50, 2000);
        throw new AssertionError("failed file write must not count as saved audio");
      } catch (IllegalStateException expected) {
        check(
            expected.getCause() instanceof ClosedByInterruptException,
            "real disk write failure must reach caller: " + expected);
      }
    } finally {
      worker.shutdownNow();
      check(worker.awaitTermination(2, TimeUnit.SECONDS), "test worker did not stop");
    }
  }

  private static void sessionUpdateMustBeConfirmed() throws Exception {
    try (Scenario scenario = new Scenario()) {
      AtomicBoolean confirmed = new AtomicBoolean();
      AtomicBoolean prematureInput = new AtomicBoolean();
      scenario.update =
          socket -> {
            socket.send("{\"type\":\"session.created\",\"session\":{\"id\":\"test-session\"}}");
            scenario.sender.schedule(
                () -> {
                  confirmed.set(true);
                  socket.send("{\"type\":\"session.updated\"}");
                },
                120,
                TimeUnit.MILLISECONDS);
          };
      scenario.firstInput = socket -> prematureInput.set(!confirmed.get());
      scenario.finalInput = socket -> audio(socket, "audio", "AQI=");
      scenario.run(50, 1000);
      check(
          confirmed.get() && !prematureInput.get(),
          "websocket/session.created is not configuration confirmation");
    }
  }

  private static void malformedAudioIsNotReportedAsSuccess() throws Exception {
    try (Scenario scenario = new Scenario()) {
      scenario.finalInput = socket -> audio(socket, "audio", "not-valid-base64!");
      try {
        scenario.run(50, 1000);
        throw new AssertionError("malformed audio cannot become successful output");
      } catch (IllegalStateException expected) {
        check(
            expected.getCause() instanceof IllegalArgumentException,
            "decoding error must reach caller");
      }
    }
  }

  private static Object staticCall(String name, Class<?>[] types, Object... args) throws Exception {
    Method method = Class.forName("QwenLiveQuickStart").getDeclaredMethod(name, types);
    method.setAccessible(true);
    try {
      return method.invoke(null, args);
    } catch (InvocationTargetException wrapped) {
      Throwable cause = wrapped.getCause();
      if (cause instanceof Exception) throw (Exception) cause;
      throw new AssertionError(cause);
    }
  }

  private static void runSample(
      QwenLiveConnectionParam param, Path input, Path output, long idle, long timeout)
      throws Exception {
    staticCall(
        "run",
        new Class<?>[] {
          QwenLiveConnectionParam.class,
          String.class,
          Path.class,
          Path.class,
          long.class,
          long.class
        },
        param,
        "test-backend",
        input,
        output,
        idle,
        timeout);
  }

  private static void doesNotOverwriteOutputAndRejectsBadWaitSettings() throws Exception {
    Path directory = Files.createTempDirectory(Paths.get("target"), "quickstart-input-");
    Path input = directory.resolve("input.pcm");
    Path output = directory.resolve("existing.pcm");
    QwenLiveConnectionParam param =
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .apikey("test-key")
            .url("http://127.0.0.1:1/realtime")
            .build();
    try {
      Files.write(input, new byte[] {1, 2});
      Files.write(output, new byte[] {9, 8});
      try {
        runSample(param, input, output, 10, 500);
        throw new AssertionError("existing output must not be overwritten");
      } catch (java.nio.file.FileAlreadyExistsException expected) {
      }
      check(
          Arrays.equals(new byte[] {9, 8}, Files.readAllBytes(output)), "existing output changed");
      try {
        runSample(param, input, output, 0, 500);
        throw new AssertionError("invalid wait settings accepted");
      } catch (IllegalArgumentException expected) {
      }
    } finally {
      Files.deleteIfExists(input);
      Files.deleteIfExists(output);
      Files.delete(directory);
    }
  }

  private static void fileToReplyLoopWaitsForDelayedAudio() throws Exception {
    Path directory = Files.createTempDirectory(Paths.get("target"), "quickstart-loop-");
    Path input = directory.resolve("input.pcm");
    Path output = directory.resolve("reply.pcm");
    Files.write(input, new byte[] {1, 2, -1, -2});
    ByteArrayOutputStream receivedInput = new ByteArrayOutputStream();
    AtomicBoolean sentFinalAudio = new AtomicBoolean();
    AtomicBoolean prematureClose = new AtomicBoolean();
    AtomicBoolean correctDelegation = new AtomicBoolean();
    ScheduledExecutorService sender = Executors.newSingleThreadScheduledExecutor();
    MockWebServer server = new MockWebServer();
    server.enqueue(
        new MockResponse()
            .withWebSocketUpgrade(
                new WebSocketListener() {
                  @Override
                  public void onMessage(WebSocket socket, String text) {
                    com.google.gson.JsonObject event = JsonUtils.parse(text);
                    String type = event.get("type").getAsString();
                    if ("session.update".equals(type)) {
                      correctDelegation.set(
                          "responses"
                              .equals(
                                  event
                                      .getAsJsonObject("session")
                                      .getAsJsonObject("delegation")
                                      .get("type")
                                      .getAsString()));
                      socket.send("{\"type\":\"session.updated\"}");
                    } else if ("session.input_audio.append".equals(type)) {
                      byte[] data = Base64.getDecoder().decode(event.get("audio").getAsString());
                      receivedInput.write(data, 0, data.length);
                      if (receivedInput.size() == 32004) {
                        socket.send("{\"type\":\"session.output_audio.delta\",\"audio\":\"BQY=\"}");
                        sender.schedule(
                            () -> {
                              sentFinalAudio.set(true);
                              socket.send(
                                  "{\"type\":\"session.output_audio.delta\",\"delta\":\"BwgJCg==\"}");
                            },
                            120,
                            TimeUnit.MILLISECONDS);
                      }
                    }
                  }

                  @Override
                  public void onClosing(WebSocket socket, int code, String reason) {
                    if (!sentFinalAudio.get()) prematureClose.set(true);
                    socket.close(code, reason);
                  }
                }));
    server.start();
    try {
      QwenLiveConnectionParam param =
          QwenLiveConnectionParam.builder()
              .model("qwen-live")
              .apikey("test-key")
              .url(server.url("/realtime").toString())
              .build();
      runSample(param, input, output, 250, 3000);
      check(correctDelegation.get(), "minimal sample must use server responses delegation");
      check(
          !prematureClose.get() && sentFinalAudio.get(),
          "sample closed before delayed final audio");
      check(
          Arrays.equals(new byte[] {5, 6, 7, 8, 9, 10}, Files.readAllBytes(output)),
          "reply PCM did not contain all fragments");
      byte[] expectedInput = new byte[32004];
      System.arraycopy(new byte[] {1, 2, -1, -2}, 0, expectedInput, 0, 4);
      check(
          Arrays.equals(expectedInput, receivedInput.toByteArray()),
          "input slice or trailing silence was encoded incorrectly");
    } finally {
      sender.shutdownNow();
      check(sender.awaitTermination(2, TimeUnit.SECONDS), "test sender did not stop");
      server.shutdown();
      Files.deleteIfExists(input);
      Files.deleteIfExists(output);
      Files.delete(directory);
    }
  }

  private static void standaloneCliExitsAfterSuccessAndFailure() throws Exception {
    Path directory = Files.createTempDirectory(Paths.get("target"), "quickstart-cli-");
    Path input = directory.resolve("input.pcm");
    Files.write(input, new byte[] {1, 2});
    MockWebServer server = new MockWebServer();
    server.start();
    try {
      for (boolean fail : new boolean[] {false, true}) {
        server.enqueue(
            new MockResponse()
                .withWebSocketUpgrade(
                    new WebSocketListener() {
                      @Override
                      public void onMessage(WebSocket socket, String text) {
                        String type = JsonUtils.parse(text).get("type").getAsString();
                        if ("session.update".equals(type)) {
                          socket.send(
                              fail
                                  ? "{\"type\":\"error\",\"error\":{\"code\":\"test_error\",\"message\":\"configuration failed\"}}"
                                  : "{\"type\":\"session.updated\"}");
                        } else if ("session.input_audio.append".equals(type)) {
                          socket.send(
                              "{\"type\":\"session.output_audio.delta\",\"audio\":\"AwQ=\"}");
                        }
                      }

                      @Override
                      public void onClosing(WebSocket socket, int code, String reason) {
                        socket.close(code, reason);
                      }
                    }));
        Path output = directory.resolve(fail ? "failed.pcm" : "reply.pcm");
        ProcessBuilder command =
            new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp",
                System.getProperty("java.class.path"),
                "QwenLiveQuickStart",
                "test-workspace",
                "test-backend",
                input.toString(),
                output.toString(),
                "1",
                "3");
        command.environment().put("DASHSCOPE_API_KEY", "test-key");
        command
            .environment()
            .put("DASHSCOPE_WEBSOCKET_QWEN_LIVE_BASE_URL", server.url("/realtime").toString());
        command.redirectErrorStream(true);
        Process process = command.start();
        try {
          check(process.waitFor(7, TimeUnit.SECONDS), "CLI hung after closing its request");
          ByteArrayOutputStream log = new ByteArrayOutputStream();
          byte[] buffer = new byte[2048];
          int read;
          while ((read = process.getInputStream().read(buffer)) != -1) log.write(buffer, 0, read);
          String text = log.toString("UTF-8");
          check(
              process.exitValue() == (fail ? 1 : 0), "CLI returned incorrect exit status: " + text);
          check(
              text.contains("Saved raw PCM:") != fail,
              "CLI reported failure as saved output: " + text);
          if (!fail) check(Files.size(output) > 0, "successful CLI did not save audio");
        } finally {
          if (process.isAlive()) process.destroyForcibly();
          process.waitFor(2, TimeUnit.SECONDS);
          Files.deleteIfExists(output);
        }
      }
    } finally {
      server.shutdown();
      Files.deleteIfExists(input);
      Files.delete(directory);
    }
  }

  private static void invalidCliArgumentsReturnFailure() throws Exception {
    Process process =
        new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp",
                System.getProperty("java.class.path"),
                "QwenLiveQuickStart")
            .redirectErrorStream(true)
            .start();
    try {
      check(process.waitFor(5, TimeUnit.SECONDS), "usage path must exit immediately");
      check(
          process.exitValue() == 2, "invalid CLI argument count must exit 2, not indicate success");
    } finally {
      if (process.isAlive()) process.destroyForcibly();
      process.waitFor(2, TimeUnit.SECONDS);
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
