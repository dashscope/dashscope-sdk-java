// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One compiled paraformer phrase set. Prefer {@link #getPhraseId()} (the resource id that {@code
 * asr.recognize} consumes) over the compile job id; the wire reports it as {@code phrase_id} /
 * {@code finetuned_output} / {@code job_id} depending on the endpoint.
 */
public final class Phrase extends ResponseBase {
  @JsonProperty("phrase_id")
  @JsonAlias({"finetuned_output", "job_id"})
  private String phraseId;

  @JsonProperty("status")
  private String status;

  public String getPhraseId() {
    return phraseId;
  }

  public String getStatus() {
    return status;
  }
}
