package com.alibaba.dashscope.audio.asr.phrase;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class AsrPhraseStatusOutput {
  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_JOB_ID)
  private String jobId;

  private String status;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_FINETUNED_OUTPUT)
  private String fineTunedOutput;

  private Integer code;

  private String model;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_OUTPUT_TYPE)
  private String outputType;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_PAGE_NO)
  private Integer pageNo;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_PAGE_SIZE)
  private Integer pageSize;

  private Integer total;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_FINETUNED_OUTPUTS)
  private List<AsrPhraseInfo> finetunedOutputs;

  @SerializedName(AsrPhraseApiKeywords.ASR_PHRASE_CREATE_TIME)
  private String createTime;
}
