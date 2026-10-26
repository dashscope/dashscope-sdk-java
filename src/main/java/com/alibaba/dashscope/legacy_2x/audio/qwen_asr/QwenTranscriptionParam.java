// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.qwen_asr;

import com.alibaba.dashscope.legacy_2x.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Slf4j
public class QwenTranscriptionParam extends HalfDuplexServiceParam {

  @NonNull private String fileUrl;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("model", getModel());

    JsonArray jsonChannelId = new JsonArray();

    JsonObject jsonInput = new JsonObject();
    jsonInput.addProperty(QwenTranscriptionApiKeywords.FILE_URL, fileUrl);
    body.add("input", jsonInput);

    JsonObject jsonParameters = JsonUtils.parametersToJsonObject(getParameters());
    body.add("parameters", jsonParameters);
    log.debug("body=>{}", body);
    return body;
  }

  @Override
  public Object getInput() {
    JsonObject jsonInput = new JsonObject();
    jsonInput.addProperty(QwenTranscriptionApiKeywords.FILE_URL, fileUrl);
    return jsonInput;
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {}
}
