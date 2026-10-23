// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.resources.jobs.JobsService;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ws.WsConnection;
import com.alibaba.dashscope.v3.types.audio.AsrRecognizeParams;
import com.alibaba.dashscope.v3.types.audio.AsrTranscribeParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@code audio.asr} resource: realtime {@code recognize} (ws duplex) and async {@code transcribe}.
 */
public final class AsrService {
  static final String TRANSCRIBE_PATH = "/services/audio/asr/transcription";
  static final String MODULE = "audio";

  private final HttpTransport transport;
  private final JobsService jobs;

  public AsrService(HttpTransport transport) {
    this.transport = transport;
    this.jobs = new JobsService(transport, MODULE);
  }

  /** Realtime recognition on a dedicated socket; lazy — nothing connects until {@code start()}. */
  public RecognitionSession recognize(AsrRecognizeParams params) {
    return recognize(params, RequestOptions.NONE);
  }

  public RecognitionSession recognize(AsrRecognizeParams params, RequestOptions options) {
    return new RecognitionSession(
        transport, transport.config().getWsBaseUrl(), params, null, options);
  }

  /**
   * Realtime recognition multiplexed on a shared connection (audio design §7). {@code close()} on
   * the returned session detaches the task only; the socket stays open.
   */
  public RecognitionSession recognize(AsrRecognizeParams params, WsConnection connection) {
    return recognize(params, connection, RequestOptions.NONE);
  }

  public RecognitionSession recognize(
      AsrRecognizeParams params, WsConnection connection, RequestOptions options) {
    return new RecognitionSession(
        transport, transport.config().getWsBaseUrl(), params, connection, options);
  }

  /**
   * Blocks until a terminal state; equivalent to jobs().create + jobs().wait.
   *
   * @see #transcribe(AsrTranscribeParams, RequestOptions)
   */
  public Job transcribe(AsrTranscribeParams params) {
    return transcribe(params, RequestOptions.NONE);
  }

  /** Blocks until a terminal state; equivalent to jobs().create + jobs().wait. */
  public Job transcribe(AsrTranscribeParams params, RequestOptions options) {
    Job created = jobs.create(TRANSCRIBE_PATH, buildTranscribeBody(params), options);
    return jobs.wait(created.getTaskId(), options);
  }

  /** The async-job handle for transcription: create/fetch/wait/cancel/list. */
  public JobsService jobs() {
    return jobs;
  }

  private static ObjectNode buildTranscribeBody(AsrTranscribeParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    ArrayNode urls = input.putArray("file_urls");
    params.getFileUrls().forEach(urls::add);
    if (params.getExtraInput() != null) {
      params.getExtraInput().forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }
    if (params.getParameters() != null && !params.getParameters().isEmpty()) {
      ObjectNode parameters = root.putObject("parameters");
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    if (params.getPhraseId() != null) {
      root.putArray("resources")
          .addObject()
          .put("resource_id", params.getPhraseId())
          .put("resource_type", "asr_phrase");
    }
    return root;
  }
}
