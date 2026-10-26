package com.alibaba.dashscope.legacy_2x.audio.asr.phrase;

import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.utils.ApiKeywords;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode()
public class AsrPhraseStatusResult {
  @SerializedName(ApiKeywords.REQUEST_ID)
  private String requestId;

  private AsrPhraseStatusOutput output;

  private String code;

  private String message;

  public static AsrPhraseStatusResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    AsrPhraseStatusResult result = new AsrPhraseStatusResult();
    if (dashScopeResult.getOutput() != null) {
      result.output =
          JsonUtils.fromJson((JsonObject) dashScopeResult.getOutput(), AsrPhraseStatusOutput.class);
    }
    result.requestId = dashScopeResult.getRequestId();
    return result;
  }

  public AsrPhraseInfo getAsrPhraseInfo() {
    AsrPhraseInfo asrPhraseInfo = new AsrPhraseInfo();
    asrPhraseInfo.setCreateTime(output.getCreateTime());
    asrPhraseInfo.setFineTunedOutput(output.getFineTunedOutput());
    asrPhraseInfo.setJobId(output.getJobId());
    asrPhraseInfo.setModel(output.getModel());
    asrPhraseInfo.setOutputType(output.getOutputType());
    return asrPhraseInfo;
  }
}
