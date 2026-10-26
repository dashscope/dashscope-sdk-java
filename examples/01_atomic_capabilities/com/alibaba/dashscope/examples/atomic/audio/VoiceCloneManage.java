// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.audio.Voice;
import com.alibaba.dashscope.types.audio.VoiceCreateParams;
import com.alibaba.dashscope.types.audio.VoiceList;
import com.alibaba.dashscope.types.common.JobStatus;
import java.util.HashMap;
import java.util.Map;

/**
 * Voice cloning management (Qwen-Audio / CosyVoice): create a clone from reference audio at a
 * service-reachable HTTPS URL and wait until it is OK, or get/list/delete existing clones. Default
 * target model {@code qwen-audio-3.0-tts-flash} (voice-enrollment API); synthesis must use the same
 * target model. No synthesis, playback, or auto-deletion here: a created voice persists, including
 * after timeout or interruption, and is never deleted unless {@code delete --yes} is run
 * explicitly. Usage:
 *
 * <pre>
 * VoiceCloneManage create --url &lt;https audio&gt; [--prefix demo] [--model qwen-audio-3.0-tts-flash] [--wait-timeout SEC]
 * VoiceCloneManage get --voice-id &lt;id&gt;
 * VoiceCloneManage list [--prefix P] [--page-index N] [--page-size N]
 * VoiceCloneManage delete --voice-id &lt;id&gt; --yes
 * </pre>
 *
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/voice_clone.py.
 */
public final class VoiceCloneManage {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      usage();
      System.exit(2);
    }
    String command = args[0];
    Map<String, String> opts = new HashMap<>();
    boolean yes = false;
    for (int i = 1; i < args.length; i++) {
      if ("--yes".equals(args[i])) {
        yes = true;
      } else if (args[i].startsWith("--")) {
        opts.put(args[i], value(args, ++i));
      } else {
        usage();
        System.exit(2);
      }
    }

    try (DashScopeClient client = ExampleSupport.client()) {
      switch (command) {
        case "create":
          create(client, opts);
          break;
        case "get":
          get(client, voiceId(opts));
          break;
        case "list":
          list(client, opts);
          break;
        case "delete":
          if (!yes) {
            System.err.println("error: delete requires --yes; nothing has been deleted");
            System.exit(2);
          }
          delete(client, voiceId(opts));
          break;
        default:
          usage();
          System.exit(2);
      }
    }
  }

  private static void create(DashScopeClient client, Map<String, String> opts) throws Exception {
    String url = opts.get("--url");
    if (url == null || !url.startsWith("https://")) {
      // Upload owned/authorized speaker audio yourself; this sample never uploads local files.
      // Recommended: 10-20 s WAV (16-bit)/MP3/M4A, >=16 kHz, <=10 MB, at most 60 s, with at
      // least 5 s of continuous clear speech and no music or other speakers.
      System.err.println("error: create requires --url <service-reachable HTTPS audio URL>");
      System.exit(2);
    }
    String prefix = opts.getOrDefault("--prefix", "demo");
    if (!prefix.matches("[a-z0-9]{1,10}")) {
      System.err.println("error: --prefix must be 1-10 lowercase letters/digits");
      System.exit(2);
    }
    String model = opts.getOrDefault("--model", "qwen-audio-3.0-tts-flash");
    long waitTimeoutSec = Long.parseLong(opts.getOrDefault("--wait-timeout", "300"));
    System.err.println(
        "Use only your own or explicitly authorized speaker audio. Creating a clone may incur"
            + " charges; it will NOT be automatically deleted.");
    Voice voice =
        client
            .audio()
            .voices()
            .create(VoiceCreateParams.builder().targetModel(model).prefix(prefix).url(url).build());
    if (voice.getVoiceId() == null) {
      throw new IllegalStateException(
          "No voice ID returned (request_id="
              + voice.getRequestId()
              + "); use list before attempting another create");
    }
    System.out.println("voice_id: " + voice.getVoiceId() + " request_id=" + voice.getRequestId());
    System.err.println("Recover without creating again: get --voice-id " + voice.getVoiceId());
    waitReady(client, voice.getVoiceId(), waitTimeoutSec);
    System.out.println(
        "Ready. Synthesize separately with the same target model ("
            + model
            + "); may incur charges.");
  }

  /** Poll until OK; only DEPLOYING is retryable — never poll UNDEPLOYED or unknown states. */
  private static void waitReady(DashScopeClient client, String voiceId, long waitTimeoutSec)
      throws Exception {
    long deadline = System.nanoTime() + waitTimeoutSec * 1_000_000_000L;
    String previous = null;
    while (true) {
      long remainingMs = (deadline - System.nanoTime()) / 1_000_000L;
      if (remainingMs <= 0) {
        throw new IllegalStateException(
            "Voice is not ready before --wait-timeout; use get to recover");
      }
      Voice voice = client.audio().voices().get(voiceId);
      if (!String.valueOf(voice.getStatus()).equals(previous)) {
        System.out.println("Voice status: " + voice.getStatus());
        previous = String.valueOf(voice.getStatus());
      }
      if ("OK".equals(voice.getStatus())) {
        return;
      }
      if (!"DEPLOYING".equals(voice.getStatus())) {
        throw new IllegalStateException(
            "Voice is unavailable or has unsupported status: " + voice.getStatus());
      }
      Thread.sleep(Math.min(2000, remainingMs));
    }
  }

  private static void get(DashScopeClient client, String voiceId) {
    Voice voice = client.audio().voices().get(voiceId);
    System.out.println(
        "voice_id: "
            + voiceId
            + "; status: "
            + voice.getStatus()
            + "; request_id="
            + voice.getRequestId());
    if (!"OK".equals(voice.getStatus())) {
      System.err.println("Voice is not ready; do not synthesize with it yet.");
      System.exit(1);
    }
  }

  private static void list(DashScopeClient client, Map<String, String> opts) {
    String prefix = opts.get("--prefix");
    int pageIndex = Integer.parseInt(opts.getOrDefault("--page-index", "0"));
    int pageSize = Integer.parseInt(opts.getOrDefault("--page-size", "10"));
    if (pageIndex < 0 || pageSize <= 0) {
      System.err.println("error: --page-index must be >= 0 and --page-size must be > 0");
      System.exit(2);
    }
    VoiceList voices = client.audio().voices().list(prefix, pageIndex, pageSize);
    System.out.println(
        "Page "
            + pageIndex
            + ": "
            + voices.getVoices().size()
            + " voice(s)"
            + " request_id="
            + voices.getRequestId());
    for (Map<String, Object> voice : voices.getVoices()) {
      System.out.println("voice_id: " + voice.get("voice_id") + "; status: " + voice.get("status"));
    }
  }

  private static void delete(DashScopeClient client, String voiceId) {
    JobStatus result = client.audio().voices().delete(voiceId);
    System.out.println(
        "Deleted "
            + voiceId
            + "; status: "
            + result.getStatus()
            + "; request_id: "
            + result.getRequestId());
  }

  private static String voiceId(Map<String, String> opts) {
    String id = opts.get("--voice-id");
    if (id == null || id.isEmpty() || id.chars().anyMatch(Character::isWhitespace)) {
      System.err.println("error: --voice-id must be nonempty and contain no spaces");
      System.exit(2);
    }
    return id;
  }

  private static String value(String[] args, int i) {
    if (i >= args.length) {
      usage();
      System.exit(2);
    }
    return args[i];
  }

  private static void usage() {
    System.err.println(
        "usage: VoiceCloneManage create --url <https audio> [--prefix demo] [--model M]"
            + " [--wait-timeout SEC]\n"
            + "       VoiceCloneManage get --voice-id <id>\n"
            + "       VoiceCloneManage list [--prefix P] [--page-index N] [--page-size N]\n"
            + "       VoiceCloneManage delete --voice-id <id> --yes");
  }
}
