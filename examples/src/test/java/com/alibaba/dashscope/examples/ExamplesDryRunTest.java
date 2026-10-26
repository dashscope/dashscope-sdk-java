// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Dry-runs every example {@code main} against a canned {@link MockWebServer} backend (design §8:
 * "compile and dry-run all examples with MockWebServer in CI"). Each example runs in a child JVM
 * (so its {@code System.exit} error paths stay isolated) with the config chain pointed at the mock:
 *
 * <ul>
 *   <li>{@code -Ddashscope.api.key=dry-run}
 *   <li>{@code -Ddashscope.base.url=<mock>} (realtime derives its ws URL from this)
 *   <li>{@code -Ddashscope.ws.base.url=<mock>/api-ws/v1/inference}
 *   <li>{@code DASHSCOPE_AGENTSTUDIO_URL=<mock>/api/v1/agentstudio/} (2.x shared service)
 * </ul>
 *
 * <p>Skipped: {@code AsrMicrophoneRecognize} (needs microphone hardware) and {@code
 * AsrJobsTranscribe} (enforces https:// download URLs, which a plain-http mock cannot serve).
 */
class ExamplesDryRunTest {
  private static final String PKG = "com.alibaba.dashscope.examples.";
  private static final long CHILD_TIMEOUT_SECONDS = 120;

  private static MockWebServer server;
  private static Path sandbox;
  private static String speechWav;

  @BeforeAll
  static void setUp() throws IOException {
    server = new MockWebServer();
    server.setDispatcher(new DryRunDispatcher(server));
    server.start();
    sandbox = Paths.get("target", "dry-run").toAbsolutePath();
    deleteRecursively(sandbox);
    Files.createDirectories(sandbox);
    Path wav = sandbox.resolve("speech.wav");
    Files.write(wav, MockWs.wavFileBytes(16000));
    speechWav = wav.toString();
  }

  @AfterAll
  static void tearDown() throws IOException {
    server.shutdown();
  }

  @TestFactory
  Stream<DynamicTest> examples() {
    List<DynamicTest> tests = new ArrayList<>();
    // 01 atomic: text/multimodal/vector domains.
    add(tests, "ChatCompletionsCreate", PKG + "atomic.chat.ChatCompletionsCreate");
    add(tests, "TokenizationCount", PKG + "atomic.chat.TokenizationCount");
    add(tests, "MultimodalChatCreate", PKG + "atomic.multimodal.MultimodalChatCreate");
    add(tests, "EmbeddingsCreate", PKG + "atomic.embeddings.EmbeddingsCreate");
    add(tests, "RerankCreate", PKG + "atomic.rerank.RerankCreate");
    add(tests, "ImagesGenerate", PKG + "atomic.images.ImagesGenerate");
    add(tests, "VideosGenerate", PKG + "atomic.videos.VideosGenerate");
    add(tests, "FilesManage", PKG + "atomic.files.FilesManage");
    add(tests, "ModelsList", PKG + "atomic.models.ModelsList");
    add(tests, "FineTunesCreate", PKG + "atomic.finetunes.FineTunesCreate");
    add(tests, "AppsCompletionsCreate", PKG + "atomic.apps.AppsCompletionsCreate");
    add(tests, "DecisionCreate", PKG + "atomic.decision.DecisionCreate");
    // 01 atomic: audio.
    add(tests, "AsrWebsocketRecognize", PKG + "atomic.audio.AsrWebsocketRecognize", speechWav);
    add(
        tests,
        "AsrHttpStreamRecognize",
        PKG + "atomic.audio.AsrHttpStreamRecognize",
        "--file",
        speechWav);
    add(
        tests,
        "TtsHttpSynthesize",
        PKG + "atomic.audio.TtsHttpSynthesize",
        "你好，这是一次干跑。",
        "out.wav",
        "--no-play");
    add(
        tests,
        "TtsHttpStreamSynthesize",
        PKG + "atomic.audio.TtsHttpStreamSynthesize",
        "你好，这是一次干跑。",
        "out.wav",
        "--no-play");
    add(
        tests,
        "TtsWebsocketConnect",
        PKG + "atomic.audio.TtsWebsocketConnect",
        "--text",
        "你好，这是第一段。",
        "--output",
        "out.wav",
        "--no-play");
    add(
        tests,
        "VoiceCloneCreate",
        PKG + "atomic.audio.VoiceCloneManage",
        "create",
        "--url",
        "https://example.com/ref.wav",
        "--prefix",
        "dry");
    add(tests, "VoiceCloneList", PKG + "atomic.audio.VoiceCloneManage", "list", "--prefix", "dry");
    add(
        tests,
        "VoiceCloneDelete",
        PKG + "atomic.audio.VoiceCloneManage",
        "delete",
        "--voice-id",
        "voice-dry",
        "--yes");
    add(
        tests,
        "TingWuJobsCreate",
        PKG + "atomic.audio.TingWuJobsCreate",
        "--url",
        "https://example.com/audio.wav");
    add(
        tests,
        "AudioConnectionReuseAsr",
        PKG + "atomic.audio.AudioConnectionReuse",
        "asr",
        speechWav,
        "--output-dir",
        "reuse-asr");
    add(
        tests,
        "AudioConnectionReuseTts",
        PKG + "atomic.audio.AudioConnectionReuse",
        "tts",
        "--output-dir",
        "reuse-tts");
    add(
        tests,
        "RealtimeOmniDialog",
        PKG + "atomic.audio.RealtimeOmniDialog",
        "--wav",
        speechWav,
        "--output",
        "omni.wav",
        "--no-play");
    add(
        tests,
        "RealtimeQwenAudioDialog",
        PKG + "atomic.audio.RealtimeQwenAudioDialog",
        "--wav",
        speechWav,
        "--output",
        "qwen-audio.wav",
        "--no-play");
    add(
        tests,
        "RealtimeTranslation",
        PKG + "atomic.audio.RealtimeTranslation",
        "--wav",
        speechWav,
        "--language",
        "en",
        "--output",
        "translation.wav",
        "--no-play");
    // 02 advanced engineering.
    add(tests, "CircuitBreakerRetry", PKG + "engineering.resilience.CircuitBreakerRetry");
    add(tests, "ClientSideCache", PKG + "engineering.cache.ClientSideCache");
    add(tests, "LocalMockDebugging", PKG + "engineering.testing.LocalMockDebugging");
    // 03 agent recipes.
    add(tests, "ToolCallLoop", PKG + "recipes.chat.ToolCallLoop");
    add(tests, "RagExpertSystem", PKG + "recipes.rag.RagExpertSystem");
    add(tests, "AgentStudioQuickStart", PKG + "recipes.agentstudio.AgentStudioQuickStart");
    add(
        tests,
        "AgentStudioDeploymentsManage",
        PKG + "recipes.agentstudio.AgentStudioDeploymentsManage");
    // 04 real-world scenarios (default offline harness modes).
    add(
        tests,
        "CustomerServiceAssistant",
        PKG + "scenarios.customerservice.CustomerServiceAssistant");
    add(tests, "ProductionRagHarness", PKG + "scenarios.rag.ProductionRagHarness");
    add(tests, "RealtimeMeetingHarness", PKG + "scenarios.meeting.RealtimeMeetingHarness");
    add(tests, "TicketTriageHarness", PKG + "scenarios.triage.TicketTriageHarness");
    return tests.stream();
  }

  private void add(List<DynamicTest> tests, String name, String className, String... args) {
    tests.add(DynamicTest.dynamicTest(name, () -> runExample(name, className, args)));
  }

  private static void runExample(String name, String className, String... args) throws Exception {
    Path workDir = Files.createDirectories(sandbox.resolve(name));
    List<String> command = new ArrayList<>();
    command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add("-Ddashscope.api.key=dry-run");
    command.add("-Ddashscope.base.url=" + server.url("/"));
    command.add("-Ddashscope.ws.base.url=" + server.url("/api-ws/v1/inference"));
    command.add(className);
    command.addAll(Arrays.asList(args));

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(workDir.toFile());
    Map<String, String> env = builder.environment();
    // Deterministic config chain: scrub ambient DashScope env, then set the dry-run values.
    env.keySet().removeIf(key -> key.startsWith("DASHSCOPE_") || key.equals("AGENTSTUDIO_URL"));
    env.put("DASHSCOPE_CONFIG", "/nonexistent/dashscope-config.toml");
    env.put("DASHSCOPE_WORKSPACE", "dry");
    env.put("DASHSCOPE_APP_ID", "app-dry");
    env.put("DASHSCOPE_AGENT_ID", "agent-dry");
    env.put("DASHSCOPE_AGENTSTUDIO_URL", server.url("/api/v1/agentstudio/").toString());

    Process process = builder.start();
    StringBuilder captured = new StringBuilder();
    Thread outPump = pump(process.getInputStream(), captured, "");
    Thread errPump = pump(process.getErrorStream(), captured, "[stderr] ");
    boolean finished = process.waitFor(CHILD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (!finished) {
      process.destroyForcibly();
    }
    outPump.join(TimeUnit.SECONDS.toMillis(10));
    errPump.join(TimeUnit.SECONDS.toMillis(10));
    String output = captured.toString();
    assertTrue(finished, name + " timed out after " + CHILD_TIMEOUT_SECONDS + "s\n" + output);
    assertEquals(
        0, process.exitValue(), () -> name + " exit=" + process.exitValue() + "\n" + output);
  }

  private static Thread pump(java.io.InputStream stream, StringBuilder target, String prefix) {
    Thread thread =
        new Thread(
            () ->
                new java.io.BufferedReader(new java.io.InputStreamReader(stream))
                    .lines()
                    .forEach(line -> appendLine(target, prefix + line)));
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  private static synchronized void appendLine(StringBuilder target, String line) {
    target.append(line).append('\n');
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(path)) {
      for (Path item : walk.sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList())) {
        Files.delete(item);
      }
    }
  }
}
