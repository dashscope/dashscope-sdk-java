// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.scenarios.meeting;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.examples.atomic.audio.AudioCommon;
import com.alibaba.dashscope.v3.errors.ApiConnectionException;
import com.alibaba.dashscope.v3.errors.Errors;
import com.alibaba.dashscope.v3.errors.SdkErrorCode;
import com.alibaba.dashscope.v3.resources.audio.AudioPacer;
import com.alibaba.dashscope.v3.resources.tingwu.TingWuRealtimeSession;
import com.alibaba.dashscope.v3.types.tingwu.TingWuRealtimeParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Real-world scenario: a concurrent realtime meeting transcription harness.
 *
 * <p>This is not "how to open one TingWu realtime session". It is an industrial-grade base for a
 * meeting platform that must transcribe many rooms at once and keep each meeting's state and
 * transcript intact even when a socket drops. It stitches four production concerns into one
 * runnable blueprint:
 *
 * <ol>
 *   <li><b>Concurrency that respects the protocol.</b> Realtime TingWu is NOT multiplexable: its
 *       wire protocol carries no task id, so one websocket is exactly one session and closes when
 *       the session ends. Concurrency therefore means one <i>dedicated</i> connection + worker per
 *       meeting, and the pool size is the admission control that caps how many live sockets — and
 *       how much blast radius — you hold open at once.
 *   <li><b>Local state machine per meeting.</b> Every room moves through PENDING -> CONNECTING ->
 *       LISTENING -> TRANSCRIBING -> COMPLETED, with RECONNECTING and DEGRADED as managed
 *       off-ramps, so a supervisor can observe and report on hundreds of rooms uniformly.
 *   <li><b>Cloud/local split for the transcript.</b> The cloud recognizes; the SDK side
 *       <i>assembles</i>. TingWu streams incremental {@code recognize-result} events that re-send
 *       the same {@code sentenceId} with growing text until it is final. {@link
 *       TranscriptAssembler} merges those partials locally, which also makes the transcript survive
 *       a mid-stream reconnect.
 *   <li><b>Rigid resilience + isolation.</b> A dropped socket ({@link ApiConnectionException})
 *       reconnects with exponential backoff; a business {@code task-failed} (bad app id, content
 *       policy) is <i>not</i> blindly retried — it degrades that one room. Either way the failure
 *       is contained: one room falling over never takes down the others.
 * </ol>
 *
 * <p>It runs <b>fully offline and deterministically by default</b> by driving the engine through a
 * scripted {@link MeetingLink} seam (the Java SDK has no websocket {@code connector=} seam like the
 * Python SDK, so the harness owns one), which replays canned event inboxes and raises exactly the
 * exceptions the real {@link TingWuRealtimeSession} raises — premature EOF becomes {@code
 * ApiConnectionException("websocket closed before speech-end")}, a payload {@code task-failed}
 * becomes the mapped business error. Set {@code MEETING_HARNESS_LIVE=1} to point the exact same
 * engine at the real gateway with {@code DASHSCOPE_API_KEY} + {@code DASHSCOPE_TINGWU_APP_ID} and a
 * PCM WAV via {@code DASHSCOPE_DEMO_AUDIO}.
 *
 * <p>Translation of the Python blueprint example
 * 04_real_world_scenarios/realtime_audio_meeting_harness.py.
 */
public final class RealtimeMeetingHarness {

  // ---- configuration (override via env) ----
  private static final String MODEL = envOr("DASHSCOPE_TINGWU_MODEL", "tingwu-turbo");
  private static final String APP_ID = envOr("DASHSCOPE_TINGWU_APP_ID", "your-app-id");
  private static final String TERMINOLOGY = System.getenv("DASHSCOPE_TINGWU_TERMINOLOGY");
  private static final boolean LIVE = "1".equals(System.getenv("MEETING_HARNESS_LIVE"));
  private static final String AUDIO_FILE = System.getenv("DASHSCOPE_DEMO_AUDIO");

  /** Bounded worker pool == bounded live sockets == admission control. */
  private static final int MAX_CONCURRENT = Integer.parseInt(envOr("MEETING_MAX_CONCURRENT", "4"));
  /** Per-meeting reconnects before degrading. */
  private static final int RECONNECT_ATTEMPTS = 2;
  /** 0 keeps the offline demo instant; use ~0.5 in prod. */
  private static final double BASE_DELAY = 0.0;

  private static final Duration SESSION_TIMEOUT = Duration.ofSeconds(30);

  private static final int SAMPLE_RATE = 16000;
  /** 100 ms of 16 kHz 16-bit mono PCM. */
  private static final int FRAME_BYTES = AudioPacer.bytesPer100ms(SAMPLE_RATE);

  // Meeting states (a plain string enum keeps the report dependency-free).
  private static final String PENDING = "pending";
  private static final String CONNECTING = "connecting";
  private static final String LISTENING = "listening";
  private static final String TRANSCRIBING = "transcribing";
  private static final String COMPLETED = "completed";
  private static final String RECONNECTING = "reconnecting";
  private static final String DEGRADED = "degraded";

  private RealtimeMeetingHarness() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  // ===================== local transcript assembly =====================

  /**
   * Merge streaming partials into final segments, keyed by sentenceId.
   *
   * <p>TingWu realtime re-sends the same {@code sentenceId} with growing text until the sentence is
   * final. Keeping the latest text per id and preserving first-seen order is the local half of the
   * cloud/local split — and because the assembler outlives a single socket, the transcript keeps
   * its place across a mid-stream reconnect.
   */
  static final class TranscriptAssembler {
    private final List<String> order = new ArrayList<>();
    private final Map<String, String> textById = new LinkedHashMap<>();

    /** Fold one recognize-result sentence in; return whether the sentence is final. */
    synchronized boolean feed(JsonNode sentence) {
      JsonNode sidNode = sentence.get("sentenceId");
      String sid;
      if (sidNode == null || sidNode.isNull()) {
        // no id: fall back to positional so nothing is lost
        sid = Integer.toString(order.size());
      } else {
        sid = sidNode.asText();
      }
      if (!textById.containsKey(sid)) {
        order.add(sid);
      }
      textById.put(sid, sentence.path("text").asText(""));
      return sentence.path("end").asBoolean(false);
    }

    synchronized List<String> segments() {
      List<String> out = new ArrayList<>();
      for (String sid : order) {
        String text = textById.get(sid);
        if (text != null && !text.isEmpty()) {
          out.add(text);
        }
      }
      return out;
    }

    String text() {
      return String.join("", segments());
    }
  }

  // ===================== meeting link seam (live / scripted) =====================

  /**
   * One dedicated realtime link per meeting attempt. The Java SDK deliberately has no {@code
   * connector=} seam for TingWu, so the harness abstracts the session: live builds a real {@link
   * TingWuRealtimeSession}, offline replays a scripted event inbox with identical failure modes.
   */
  interface MeetingLink extends AutoCloseable {
    /** Connect and block until task-started. */
    void start();

    void sendAudioFrame(byte[] frame);

    /** Signal end of audio; results keep arriving until speech-end. */
    void stop();

    /** Raw event frames, ending at the speech-end action (inclusive). */
    Iterable<JsonNode> events();

    @Override
    void close();
  }

  /** Live link: delegates to the real TingWu realtime session (a dedicated websocket). */
  static final class LiveMeetingLink implements MeetingLink {
    private final TingWuRealtimeSession session;

    LiveMeetingLink(TingWuRealtimeSession session) {
      this.session = session;
    }

    @Override
    public void start() {
      session.start();
    }

    @Override
    public void sendAudioFrame(byte[] frame) {
      session.sendAudioFrame(frame);
    }

    @Override
    public void stop() {
      session.stop();
    }

    @Override
    public Iterable<JsonNode> events() {
      return session.events();
    }

    @Override
    public void close() {
      session.close();
    }
  }

  /**
   * A scripted duplex link that replays a canned event inbox.
   *
   * <p>Mirrors the real session's contract: {@link #start()} consumes the {@code task-started}
   * frame; {@link #events()} yields frames until {@code speech-end} (inclusive). An inbox that
   * stops short of {@code speech-end} simulates a mid-stream socket drop and raises {@link
   * ApiConnectionException}, so the reconnect path is exercised; a payload {@code task-failed}
   * raises the mapped business error, so the degrade path is exercised.
   */
  static final class ScriptedMeetingLink implements MeetingLink {
    private final List<JsonNode> inbox;
    private int cursor;
    private boolean terminal;

    ScriptedMeetingLink(List<JsonNode> inbox) {
      this.inbox = inbox;
    }

    @Override
    public void start() {
      if (cursor >= inbox.size()) {
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed before task-started", null);
      }
      JsonNode first = inbox.get(cursor++);
      raiseIfTaskFailed(first);
      String event = first.path("header").path("event").asText("");
      if (!"task-started".equals(event)) {
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed before task-started", null);
      }
    }

    @Override
    public void sendAudioFrame(byte[] frame) {
      // Scripted link: audio is accepted and dropped; the canned inbox is the fixture.
    }

    @Override
    public void stop() {
      // Scripted link: end-of-audio is accepted; the canned inbox drives the rest.
    }

    @Override
    public Iterable<JsonNode> events() {
      return () ->
          new Iterator<JsonNode>() {
            @Override
            public boolean hasNext() {
              if (terminal) {
                return false;
              }
              if (cursor < inbox.size()) {
                return true;
              }
              // Premature EOF: the real session raises here when the socket drops mid-stream.
              throw Errors.connection(
                  SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed before speech-end", null);
            }

            @Override
            public JsonNode next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              JsonNode frame = inbox.get(cursor++);
              raiseIfTaskFailed(frame);
              String action = frame.path("payload").path("output").path("action").asText("");
              if ("speech-end".equals(action)) {
                terminal = true;
              }
              return frame;
            }
          };
    }

    private static void raiseIfTaskFailed(JsonNode frame) {
      JsonNode output = frame.path("payload").path("output");
      if (!"task-failed".equals(output.path("action").asText(""))) {
        return;
      }
      String code = output.path("errorCode").isTextual() ? output.path("errorCode").asText() : null;
      String message =
          output.path("errorMessage").isTextual()
              ? output.path("errorMessage").asText()
              : "task failed";
      throw Errors.fromBusinessCode(code, message, "r", null, null);
    }

    @Override
    public void close() {
      terminal = true;
    }
  }

  /** Opens one link per meeting attempt. */
  interface MeetingGateway {
    MeetingLink open(String meetingId);

    default void close() {}
  }

  /**
   * Serve a per-meeting, per-attempt event script.
   *
   * <p>Each {@code open} builds a fresh {@link ScriptedMeetingLink} from the next scripted inbox
   * for that meeting, so a meeting whose first attempt drops can be given a completing second
   * attempt — exactly the reconnect-and-resume flow the harness is meant to survive.
   */
  static final class MockMeetingGateway implements MeetingGateway {
    private final Map<String, List<List<JsonNode>>> scripts;
    private final Map<String, Integer> attempts = new LinkedHashMap<>();

    MockMeetingGateway(Map<String, List<List<JsonNode>>> scripts) {
      this.scripts = scripts;
    }

    @Override
    public MeetingLink open(String meetingId) {
      int attempt = attempts.getOrDefault(meetingId, 0);
      attempts.put(meetingId, attempt + 1);
      List<List<JsonNode>> script = scripts.get(meetingId);
      return new ScriptedMeetingLink(script.get(Math.min(attempt, script.size() - 1)));
    }
  }

  /** Real gateway: the SDK opens a real websocket per session. */
  static final class LiveMeetingGateway implements MeetingGateway {
    private final DashScopeClient client;

    LiveMeetingGateway(String apiKey) {
      this.client = DashScopeClient.builder().apiKey(apiKey).timeout(SESSION_TIMEOUT).build();
    }

    @Override
    public MeetingLink open(String meetingId) {
      TingWuRealtimeParams.Builder params =
          TingWuRealtimeParams.builder()
              .model(MODEL)
              .appId(APP_ID)
              .format("pcm")
              .sampleRate(SAMPLE_RATE);
      if (TERMINOLOGY != null && !TERMINOLOGY.isEmpty()) {
        params.terminology(TERMINOLOGY);
      }
      return new LiveMeetingLink(client.tingwu().realtime(params.build()));
    }

    @Override
    public void close() {
      client.close();
    }
  }

  // ===================== per-meeting state machine =====================

  /** One meeting: identity, state, transcript, and retry bookkeeping. */
  static final class MeetingSession {
    final String meetingId;
    final String room;
    final List<byte[]> frames;
    /** True only for the live path (realtime cadence). */
    final boolean pace;

    volatile String state = PENDING;
    final TranscriptAssembler assembler = new TranscriptAssembler();
    int attempts;
    int reconnects;
    volatile Exception error;

    MeetingSession(String meetingId, String room, List<byte[]> frames, boolean pace) {
      this.meetingId = meetingId;
      this.room = room;
      this.frames = frames;
      this.pace = pace;
    }

    /**
     * Move to {@code state} and log it, tagged so interleaving is readable.
     *
     * <p>Meetings run on worker threads, so their log lines interleave; the {@code [meeting_id]}
     * prefix is what makes a concurrent run still traceable per room.
     */
    void transition(String state, String note) {
      this.state = state;
      String suffix = note == null || note.isEmpty() ? "" : " :: " + note;
      System.out.println("  [" + meetingId + "] -> " + state + suffix);
    }
  }

  /**
   * Only a dropped/timed-out socket is worth reopening.
   *
   * <p>A {@code task-failed} event (bad app id, content policy, parameter error) surfaces as a
   * non-connection DashScopeException; reconnecting would just fail again, so those degrade instead
   * of retry. A raw socket timeout from the websocket layer is classified by name.
   */
  private static boolean isReconnectable(Exception exc) {
    if (exc instanceof ApiConnectionException) {
      return true;
    }
    String name = exc.getClass().getSimpleName();
    return name.contains("Timeout") || name.contains("ConnectionClosed");
  }

  private static void handleEvent(MeetingSession session, JsonNode event) {
    JsonNode output = event.path("payload").path("output");
    if (!"recognize-result".equals(output.path("action").asText(""))) {
      return; // speech-end just terminates the iterator; nothing to store
    }
    JsonNode sentence = output.path("sentence");
    boolean isEnd = session.assembler.feed(sentence);
    String marker = isEnd ? "final" : "part ";
    System.out.println(
        "  [" + session.meetingId + "] " + marker + " | " + sentence.path("text").asText(""));
  }

  /**
   * Send every frame, then signal end of audio (stop).
   *
   * <p>Production variant: to transcribe <i>live</i> rather than after stop, drain {@code events()}
   * on a reader thread while frames are still being sent — the socket is duplex. The sequential
   * shape here matches 01_atomic_capabilities/audio and keeps the offline mock deterministic.
   */
  private static void pumpAudio(MeetingSession session, MeetingLink link) {
    long start = System.nanoTime();
    int index = 0;
    for (byte[] frame : session.frames) {
      if (session.pace) {
        // only the live path paces to realtime cadence
        AudioPacer.pace(start, index);
      }
      index++;
      link.sendAudioFrame(frame);
    }
    link.stop();
  }

  /** Open one dedicated socket and drive the meeting over it. */
  private static void runOnce(MeetingSession session, MeetingGateway gateway) {
    session.transition(CONNECTING, "");
    try (MeetingLink link = gateway.open(session.meetingId)) {
      // start() already waited for task-started, so audio may flow.
      link.start();
      session.transition(LISTENING, "");
      pumpAudio(session, link);
      session.transition(TRANSCRIBING, "");
      for (JsonNode event : link.events()) {
        handleEvent(session, event);
      }
    }
  }

  /**
   * Drive one meeting to a terminal state; never raises.
   *
   * <p>Containing every failure inside the per-meeting worker is what makes the supervisor's
   * isolation guarantee real: a socket drop reconnects with backoff, anything else degrades this
   * room and returns, leaving the other meetings untouched.
   */
  static MeetingSession runMeeting(MeetingSession session, MeetingGateway gateway) {
    while (true) {
      session.attempts++;
      try {
        runOnce(session, gateway);
        session.transition(COMPLETED, session.assembler.segments().size() + " segment(s)");
        return session;
      } catch (Exception exc) { // isolated per meeting by design
        session.error = exc;
        boolean reconnectable = isReconnectable(exc);
        if (reconnectable && session.reconnects < RECONNECT_ATTEMPTS) {
          session.reconnects++;
          double delay = BASE_DELAY * Math.pow(2.0, session.reconnects - 1);
          session.transition(
              RECONNECTING,
              String.format(
                  "%s backoff %.2fs (attempt %d/%d)",
                  exc.getClass().getSimpleName(), delay, session.reconnects, RECONNECT_ATTEMPTS));
          if (delay > 0.0) {
            try {
              Thread.sleep((long) (delay * 1000.0));
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
            }
          }
          continue;
        }
        String reason = exc.getClass().getSimpleName();
        if (!reconnectable) {
          reason += " (business failure, not reconnectable)";
        }
        session.transition(DEGRADED, reason + ": " + exc.getMessage());
        return session;
      }
    }
  }

  // ===================== concurrency supervisor base =====================

  /**
   * Run many meetings concurrently over bounded dedicated sockets.
   *
   * <p>The pool size is deliberately the concurrency <i>and</i> the admission cap: because each
   * realtime session owns a socket + worker, holding the pool bounded is what keeps a burst of join
   * requests from opening an unbounded number of live connections.
   */
  static final class MeetingSupervisor {
    private final MeetingGateway gateway;
    private final int maxConcurrent;

    MeetingSupervisor(MeetingGateway gateway, int maxConcurrent) {
      this.gateway = gateway;
      this.maxConcurrent = maxConcurrent;
    }

    List<MeetingSession> run(List<MeetingSession> meetings) {
      ExecutorService pool = Executors.newFixedThreadPool(maxConcurrent);
      try {
        List<Future<MeetingSession>> futures = new ArrayList<>();
        for (MeetingSession meeting : meetings) {
          futures.add(pool.submit(() -> runMeeting(meeting, gateway)));
        }
        List<MeetingSession> results = new ArrayList<>();
        for (Future<MeetingSession> future : futures) {
          try {
            results.add(future.get()); // runMeeting never raises
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for meetings", e);
          } catch (ExecutionException e) {
            throw new IllegalStateException("meeting worker raised unexpectedly", e);
          }
        }
        return results;
      } finally {
        pool.shutdown();
      }
    }
  }

  // ===================== scripted event builders =====================

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode event(String name, ObjectNode output) {
    ObjectNode root = MAPPER.createObjectNode();
    ObjectNode header = root.putObject("header");
    header.put("event", name);
    header.put("task_id", "t");
    header.put("request_id", "r");
    ObjectNode payload = root.putObject("payload");
    if (output != null) {
      payload.set("output", output);
    }
    return root;
  }

  private static JsonNode recognize(String text, String sentenceId, boolean end) {
    ObjectNode output = MAPPER.createObjectNode();
    output.put("action", "recognize-result");
    ObjectNode sentence = output.putObject("sentence");
    sentence.put("text", text);
    sentence.put("sentenceId", sentenceId);
    sentence.put("end", end);
    return event("result-generated", output);
  }

  private static final JsonNode STARTED = event("task-started", null);
  private static final JsonNode SPEECH_END;

  static {
    ObjectNode output = MAPPER.createObjectNode();
    output.put("action", "speech-end");
    SPEECH_END = event("result-generated", output);
  }

  // ===================== audio frame source =====================

  private static List<byte[]> mockFrames(int count) {
    List<byte[]> frames = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      frames.add(new byte[FRAME_BYTES]);
    }
    return frames;
  }

  // ===================== report =====================

  private static void report(List<MeetingSession> results) {
    System.out.println("\n======================================================================");
    System.out.println("会议托管报告 / Meeting Supervisor Report");
    System.out.println("======================================================================");
    for (MeetingSession session : results) {
      String full = session.assembler.text();
      String preview = full.length() > 36 ? full.substring(0, 36) + "…" : full;
      System.out.printf(
          "  %-12s state=%-13s attempts=%d reconnects=%d segments=%d%n",
          session.meetingId,
          session.state,
          session.attempts,
          session.reconnects,
          session.assembler.segments().size());
      System.out.println("      room      : " + session.room);
      System.out.println("      transcript: " + (preview.isEmpty() ? "(none)" : preview));
      if (session.error != null) {
        System.out.println("      last error: " + session.error.getClass().getSimpleName());
      }
    }
    int completed = 0;
    int degraded = 0;
    for (MeetingSession session : results) {
      if (COMPLETED.equals(session.state)) {
        completed++;
      } else if (DEGRADED.equals(session.state)) {
        degraded++;
      }
    }
    System.out.println("----------------------------------------------------------------------");
    System.out.printf(
        "  %d completed / %d degraded / %d total -- one room failing never took down the others.%n",
        completed, degraded, results.size());
    System.out.println("======================================================================");
  }

  // ===================== demo scenarios =====================

  /**
   * Four rooms that together exercise every resilience path.
   *
   * <p>alpha/beta are clean; gamma drops mid-stream on attempt 0 and resumes on attempt 1 (the
   * partial sentence {@code s2} is completed after the reconnect, proving the assembler survives
   * it); delta hits a business {@code task-failed} that must degrade, not reconnect.
   */
  private static Map<String, List<List<JsonNode>>> offlineScripts() {
    ObjectNode deltaFailure = MAPPER.createObjectNode();
    deltaFailure.put("action", "task-failed");
    deltaFailure.put("errorCode", "DataInspectionFailed");
    deltaFailure.put("errorMessage", "content policy blocked this room");

    Map<String, List<List<JsonNode>>> scripts = new LinkedHashMap<>();
    scripts.put(
        "room-alpha",
        Arrays.asList(
            Arrays.asList(
                STARTED,
                recognize("大家好，欢迎参加本次评审。", "s1", true),
                recognize("我们先同步一下进度。", "s2", true),
                SPEECH_END)));
    scripts.put(
        "room-beta",
        Arrays.asList(
            Arrays.asList(
                STARTED,
                recognize("线上的同学能听到吗？", "s1", true),
                recognize("可以，我们开始吧。", "s2", true),
                recognize("先请产品同学介绍背景。", "s3", true),
                SPEECH_END)));
    scripts.put(
        "room-gamma",
        Arrays.asList(
            // attempt 0: socket drops mid-sentence (no speech-end).
            Arrays.asList(
                STARTED, recognize("这个季度的目标很明确。", "s1", true), recognize("今天我们讨论", "s2", false)),
            // attempt 1: resumes and completes sentence s2, then ends.
            Arrays.asList(
                STARTED,
                recognize("今天我们讨论第三季度的路线图。", "s2", true),
                recognize("结论下周同步给全员。", "s3", true),
                SPEECH_END)));
    scripts.put(
        "room-delta",
        Arrays.asList(Arrays.asList(STARTED, event("result-generated", deltaFailure))));
    return scripts;
  }

  private static void runOfflineDemo() {
    System.out.printf("离线并发实时会议转写演示（%d 路并发上限，每场会议独占一条 websocket）%n%n", MAX_CONCURRENT);
    Map<String, List<List<JsonNode>>> scripts = offlineScripts();
    MockMeetingGateway gateway = new MockMeetingGateway(scripts);
    List<MeetingSession> meetings = new ArrayList<>();
    for (String meetingId : scripts.keySet()) {
      String suffix = meetingId.substring(meetingId.indexOf('-') + 1);
      String room = "Room " + Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1);
      meetings.add(new MeetingSession(meetingId, room, mockFrames(3), false));
    }
    MeetingSupervisor supervisor = new MeetingSupervisor(gateway, MAX_CONCURRENT);
    long started = System.nanoTime();
    List<MeetingSession> results = supervisor.run(meetings);
    double elapsed = (System.nanoTime() - started) / 1_000_000_000.0;
    System.out.printf("%n并发跑完 %d 场会议，用时 %.3fs%n", results.size(), elapsed);
    report(results);
  }

  private static void runLive() throws Exception {
    if (APP_ID.isEmpty() || "your-app-id".equals(APP_ID)) {
      System.out.println(
          "MEETING_HARNESS_LIVE=1 需要设置 DASHSCOPE_TINGWU_APP_ID （听悟应用 ID）与 DASHSCOPE_API_KEY。");
      return;
    }
    if (AUDIO_FILE == null || !Files.exists(Paths.get(AUDIO_FILE))) {
      System.out.println(
          "找不到音频文件：" + AUDIO_FILE + "（可用 DASHSCOPE_DEMO_AUDIO 指定 16kHz 单声道 16-bit WAV）");
      return;
    }
    AudioCommon.PcmAudio audio = AudioCommon.readWav(Paths.get(AUDIO_FILE), SAMPLE_RATE);
    List<byte[]> frames = new ArrayList<>();
    for (byte[] chunk : AudioPacer.pcmChunks(audio.pcm, FRAME_BYTES)) {
      frames.add(chunk);
    }
    System.out.printf("实时会议转写（live）：%d 帧 x %dB，paced%n", frames.size(), FRAME_BYTES);
    LiveMeetingGateway gateway = new LiveMeetingGateway(ExampleSupport.requireApiKey());
    try {
      MeetingSession meeting = new MeetingSession("live-room", "Live Room", frames, true);
      report(Arrays.asList(runMeeting(meeting, gateway)));
    } finally {
      gateway.close();
    }
  }

  /** Offline scripted demo by default; MEETING_HARNESS_LIVE=1 for the real gateway. */
  public static void main(String[] args) throws Exception {
    if (LIVE) {
      runLive();
    } else {
      runOfflineDemo();
    }
  }
}
