package com.alibaba.dashscope.audio.asr.phrase;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AsrPhraseInfo {
  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_CREATE_TIME)
  private String createTime;

  private String model;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_FINETUNED_OUTPUT)
  private String fineTunedOutput;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_JOB_ID)
  private String jobId;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_OUTPUT_TYPE)
  private String outputType;
}
