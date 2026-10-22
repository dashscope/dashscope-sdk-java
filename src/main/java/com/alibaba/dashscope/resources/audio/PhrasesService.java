// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.types.audio.Phrase;
import com.alibaba.dashscope.types.audio.PhraseCreateParams;
import com.alibaba.dashscope.types.audio.PhraseList;
import com.alibaba.dashscope.types.common.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code audio.phrases} — paraformer hot phrases (audio design §1.4, Python v2 phrases parity):
 * a compile job in disguise. {@code create}/{@code update} POST {@code /fine-tunes} with {@code
 * training_type=compile_asr_phrase} and an empty file set; {@code get}/{@code list}/{@code
 * delete} hit {@code /fine-tunes/outputs}. The result's {@code phraseId} (wire aliases {@code
 * phrase_id}/{@code finetuned_output}/{@code job_id}) is what {@code asr.recognize} consumes.
 */
public final class PhrasesService {
  static final String CREATE_PATH = "/fine-tunes";
  static final String OUTPUTS_PATH = "/fine-tunes/outputs";

  private final HttpTransport transport;

  public PhrasesService(HttpTransport transport) {
    this.transport = transport;
  }

  /** Compile a new phrase set; the result carries {@code phraseId}. */
  public Phrase create(PhraseCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Phrase create(PhraseCreateParams params, RequestOptions options) {
    return CustomizationSupport.parse(
        transport.execute(
            CREATE_PATH, compileBody(params, null), CustomizationSupport.MODULE, options),
        Phrase.class);
  }

  /** Recompile the phrase set behind {@code phraseId}. */
  public Phrase update(String phraseId, PhraseCreateParams params) {
    return update(phraseId, params, RequestOptions.NONE);
  }

  public Phrase update(String phraseId, PhraseCreateParams params, RequestOptions options) {
    CustomizationSupport.requireId(phraseId, "phrase_id");
    return CustomizationSupport.parse(
        transport.execute(
            CREATE_PATH, compileBody(params, phraseId), CustomizationSupport.MODULE, options),
        Phrase.class);
  }

  /** Fetch one compiled phrase set (1.x {@code query_phrases}). */
  public Phrase get(String phraseId) {
    return get(phraseId, RequestOptions.NONE);
  }

  public Phrase get(String phraseId, RequestOptions options) {
    CustomizationSupport.requireId(phraseId, "phrase_id");
    return CustomizationSupport.parse(
        transport.executeGet(OUTPUTS_PATH + "/" + phraseId, null, CustomizationSupport.MODULE,
            options),
        Phrase.class);
  }

  /** List compiled phrase sets; pages are 1-based ({@code page_no}) on this endpoint. */
  public PhraseList list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, RequestOptions.NONE);
  }

  public PhraseList list(int pageNo, int pageSize, RequestOptions options) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("page_no", String.valueOf(pageNo));
    query.put("page_size", String.valueOf(pageSize));
    return CustomizationSupport.parse(
        transport.executeGet(OUTPUTS_PATH, query, CustomizationSupport.MODULE, options),
        PhraseList.class);
  }

  /** Delete one compiled phrase set. */
  public JobStatus delete(String phraseId) {
    return delete(phraseId, RequestOptions.NONE);
  }

  public JobStatus delete(String phraseId, RequestOptions options) {
    CustomizationSupport.requireId(phraseId, "phrase_id");
    return CustomizationSupport.parse(
        transport.executeDelete(
            OUTPUTS_PATH + "/" + phraseId, CustomizationSupport.MODULE, options),
        JobStatus.class);
  }

  private static ObjectNode compileBody(PhraseCreateParams params, String finetunedOutput) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    root.putArray("training_file_ids");
    root.putArray("validation_file_ids");
    root.put("training_type", params.getTrainingType());
    ObjectNode hyper = root.putObject("hyper_parameters");
    hyper.set("phrase_list", mapper.valueToTree(params.getPhrases()));
    if (finetunedOutput != null) {
      root.put("finetuned_output", finetunedOutput);
    }
    return root;
  }
}
