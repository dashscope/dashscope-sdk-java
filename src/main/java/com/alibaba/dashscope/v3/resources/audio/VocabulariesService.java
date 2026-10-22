// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.types.audio.Vocabulary;
import com.alibaba.dashscope.v3.types.audio.VocabularyCreateParams;
import com.alibaba.dashscope.v3.types.audio.VocabularyList;
import com.alibaba.dashscope.v3.types.common.JobStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code audio.vocabularies} — asr hot-word tables (audio design §1.4, Python v2 customization
 * parity). All verbs POST the customization endpoint with an action word; {@code update}/{@code
 * delete} return {@link JobStatus}.
 */
public final class VocabulariesService {
  static final String PATH = "/services/audio/asr/customization";
  static final String DEFAULT_MODEL = "speech-biasing";

  private final HttpTransport transport;

  public VocabulariesService(HttpTransport transport) {
    this.transport = transport;
  }

  /** Create a table; the result carries {@code vocabularyId}. */
  public Vocabulary create(VocabularyCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Vocabulary create(VocabularyCreateParams params, RequestOptions options) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("target_model", params.getTargetModel());
    fields.put("prefix", params.getPrefix());
    fields.put("vocabulary", params.getVocabulary());
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(
                DEFAULT_MODEL, params.getModel(), "create_vocabulary", fields,
                params.getParameters()),
            CustomizationSupport.MODULE,
            options),
        Vocabulary.class);
  }

  /** List tables, optionally narrowed to one {@code prefix}; pages are 0-based on the wire. */
  public VocabularyList list(String prefix, int pageIndex, int pageSize) {
    return list(prefix, pageIndex, pageSize, null, RequestOptions.NONE);
  }

  public VocabularyList list(
      String prefix, int pageIndex, int pageSize, String model, RequestOptions options) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("page_index", pageIndex);
    fields.put("page_size", pageSize);
    if (prefix != null && !prefix.isEmpty()) {
      fields.put("prefix", prefix);
    }
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "list_vocabulary", fields, null),
            CustomizationSupport.MODULE,
            options),
        VocabularyList.class);
  }

  /** Fetch one table's contents (1.x {@code query_vocabulary}). */
  public Vocabulary get(String vocabularyId) {
    return get(vocabularyId, null, RequestOptions.NONE);
  }

  public Vocabulary get(String vocabularyId, String model, RequestOptions options) {
    CustomizationSupport.requireId(vocabularyId, "vocabulary_id");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("vocabulary_id", vocabularyId);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "query_vocabulary", fields, null),
            CustomizationSupport.MODULE,
            options),
        Vocabulary.class);
  }

  /** Replace one table's contents. */
  public JobStatus update(String vocabularyId, List<Map<String, Object>> vocabulary) {
    return update(vocabularyId, vocabulary, null, RequestOptions.NONE);
  }

  public JobStatus update(
      String vocabularyId, List<Map<String, Object>> vocabulary, String model,
      RequestOptions options) {
    CustomizationSupport.requireId(vocabularyId, "vocabulary_id");
    if (vocabulary == null || vocabulary.isEmpty()) {
      throw new IllegalArgumentException("vocabulary must be a non-empty list");
    }
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("vocabulary_id", vocabularyId);
    fields.put("vocabulary", vocabulary);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "update_vocabulary", fields, null),
            CustomizationSupport.MODULE,
            options),
        JobStatus.class);
  }

  /** Delete one table. */
  public JobStatus delete(String vocabularyId) {
    return delete(vocabularyId, null, RequestOptions.NONE);
  }

  public JobStatus delete(String vocabularyId, String model, RequestOptions options) {
    CustomizationSupport.requireId(vocabularyId, "vocabulary_id");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("vocabulary_id", vocabularyId);
    return CustomizationSupport.parse(
        transport.execute(
            PATH,
            CustomizationSupport.body(DEFAULT_MODEL, model, "delete_vocabulary", fields, null),
            CustomizationSupport.MODULE,
            options),
        JobStatus.class);
  }
}
