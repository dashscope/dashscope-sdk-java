// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.resources.jobs.JobResult;
import com.alibaba.dashscope.types.audio.AsrTranscribeParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * HTTP recorded-file ASR: submit HTTPS audio URLs as a batch transcription job, wait for the
 * terminal state, then print each file's transcript. Default model {@code
 * qwen-audio-3.1-asr-flash-filetrans} (accepts exactly one URL per task). Upload your own WAV to a
 * service-reachable HTTPS URL first; local paths are NOT file_urls and this sample never uploads
 * audio. Submitting is a paid operation: the task_id is printed BEFORE waiting, so an interrupted
 * run can be resumed with {@code --task-id} (same region/workspace) instead of a new paid
 * submission. Usage:
 *
 * <pre>
 * AsrJobsTranscribe (--url &lt;https audio url&gt; [--url ...] | --task-id &lt;id&gt;) [--model M] [--wait-timeout SEC]
 * </pre>
 *
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/asr_file.py.
 */
public final class AsrJobsTranscribe {

  /** Same path {@code AsrService.transcribe} submits to (its path constant is SDK-internal). */
  private static final String TRANSCRIBE_PATH = "/services/audio/asr/transcription";

  public static void main(String[] args) throws Exception {
    List<String> urls = new ArrayList<>();
    String taskId = null;
    String model = AsrTranscribeParams.DEFAULT_MODEL;
    long waitTimeoutSec = 600;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--url":
          urls.add(httpsUrl(value(args, ++i)));
          break;
        case "--task-id":
          taskId = value(args, ++i);
          break;
        case "--model":
          model = value(args, ++i);
          break;
        case "--wait-timeout":
          waitTimeoutSec = Long.parseLong(value(args, ++i));
          break;
        default:
          usage();
          System.exit(2);
      }
    }
    boolean submit = !urls.isEmpty();
    boolean resume = taskId != null && !taskId.isEmpty();
    // Exactly one of submit (--url) / resume (--task-id).
    if (submit == resume) {
      usage();
      System.exit(2);
    }
    if (submit && AsrTranscribeParams.DEFAULT_MODEL.equals(model) && urls.size() != 1) {
      System.err.println("error: qwen-audio-3.1-asr-flash-filetrans requires exactly one --url");
      System.exit(2);
    }

    try (DashScopeClient client = ExampleSupport.client()) {
      Job job;
      if (submit) {
        System.err.println("Submitting a paid batch task; input URLs will not be logged.");
        AsrTranscribeParams params =
            AsrTranscribeParams.builder().model(model).fileUrls(urls).build();
        // Not the blocking transcribe() sugar: the task_id must be printed BEFORE the wait so an
        // interrupted paid batch can be resumed with --task-id.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("model", params.getModel());
        ArrayNode fileUrls = body.putObject("input").putArray("file_urls");
        params.getFileUrls().forEach(fileUrls::add);
        Job created =
            client.audio().asr().jobs().create(TRANSCRIBE_PATH, body, RequestOptions.NONE);
        if (created.getTaskId() == null) {
          throw new IllegalStateException(
              "No task ID returned (request_id="
                  + created.getRequestId()
                  + "); check the service before submitting again");
        }
        System.out.println("task_id: " + created.getTaskId());
        System.err.println(
            "Resume without a new paid submission: --task-id " + created.getTaskId());
        job =
            client
                .audio()
                .asr()
                .jobs()
                .wait(created.getTaskId(), Duration.ofSeconds(waitTimeoutSec), RequestOptions.NONE);
      } else {
        System.out.println("task_id: " + taskId);
        job =
            client
                .audio()
                .asr()
                .jobs()
                .wait(taskId, Duration.ofSeconds(waitTimeoutSec), RequestOptions.NONE);
      }
      System.out.println(
          "Task status: "
              + job.getTaskStatus()
              + " request_id="
              + job.getRequestId()
              + (job.getUsage() != null ? " usage=" + job.getUsage() : ""));
      // Even a failed batch may contain usable completed files.
      int failures = printResults(job);
      if (!job.isSucceeded() || failures > 0) {
        System.err.println("Batch unsuccessful; " + failures + " per-file failure(s).");
        System.exit(1);
      }
    }
  }

  private static int printResults(Job job) throws Exception {
    List<JobResult> results = job.getResults();
    if (results.isEmpty()) {
      throw new IllegalStateException("Task returned no per-file results");
    }
    ObjectMapper mapper = new ObjectMapper();
    // Never reuse the SDK transport for downloads: signed URLs need no API key. OkHttp follows
    // redirects by default, matching the python.org/requests behavior of the blueprint.
    OkHttpClient downloader =
        new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(30)).build();
    int failures = 0;
    int index = 0;
    for (JobResult result : results) {
      index++;
      try {
        Object status = result.getExtra().get("subtask_status");
        if (!"SUCCEEDED".equals(status)) {
          throw new IllegalStateException("subtask did not succeed");
        }
        String url = httpsUrl(result.getTranscriptionUrl());
        try (Response response =
            downloader.newCall(new Request.Builder().url(url).get().build()).execute()) {
          if (!response.isSuccessful()) {
            throw new IllegalStateException("download failed");
          }
          System.out.println("File " + index + ": " + url);
          System.out.println(
              transcriptText(
                  mapper.readTree(response.body() == null ? "" : response.body().string())));
        }
      } catch (Exception e) {
        failures++;
        // Exception messages can contain presigned URLs; keep them out of logs.
        System.err.println("File " + index + ": FAILED (" + e.getClass().getSimpleName() + ")");
      }
    }
    return failures;
  }

  private static String transcriptText(JsonNode document) {
    JsonNode transcripts = document == null ? null : document.get("transcripts");
    if (transcripts == null || !transcripts.isArray()) {
      throw new IllegalArgumentException("transcription JSON has no transcripts list");
    }
    List<String> texts = new ArrayList<>();
    for (JsonNode transcript : transcripts) {
      JsonNode text = transcript.get("text");
      if (text != null && text.isTextual()) {
        texts.add(text.asText());
        continue;
      }
      StringBuilder joined = new StringBuilder();
      for (JsonNode sentence : transcript.path("sentences")) {
        joined.append(sentence.path("text").asText());
      }
      texts.add(joined.toString());
    }
    return String.join("\n", texts);
  }

  private static String httpsUrl(String value) {
    if (value == null || !value.startsWith("https://") || value.contains("#")) {
      throw new IllegalArgumentException(
          "Use an HTTPS audio URL without userinfo or fragments, not a local path. "
              + "Upload your own WAV to a service-reachable URL yourself; this sample never "
              + "uploads files.");
    }
    return value;
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
        "usage: AsrJobsTranscribe (--url <https audio url> [--url ...] | --task-id <id>)"
            + " [--model M] [--wait-timeout SEC]");
  }
}
