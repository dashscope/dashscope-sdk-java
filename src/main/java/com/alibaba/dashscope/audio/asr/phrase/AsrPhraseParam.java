package com.alibaba.dashscope.audio.asr.phrase;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class AsrPhraseParam extends HalfDuplexServiceParam {

  private static final String TRAIN_TYPE = "compile_asr_phrase";

  private AsrPhraseOperationType operationType;

  private String finetunedOutput;

  private Map<String, Integer> phraseList;

  private Integer pageNo;

  private Integer pageSize;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("model", getModel());
    body.addProperty(AsrPhraseApiKeywords.ASR_PHRASE_TRAINING_TYPE, TRAIN_TYPE);

    JsonObject hyperParams = new JsonObject();
    if (phraseList != null) {
      JsonObject phraseList = new JsonObject();
      for (Map.Entry<String, Integer> entry : this.phraseList.entrySet()) {
        phraseList.addProperty(entry.getKey(), entry.getValue());
      }
      hyperParams.add(AsrPhraseApiKeywords.ASR_PHRASE_LIST, phraseList);
    }

    body.add(AsrPhraseApiKeywords.ASR_PHRASE_HYPER_PARAMETERS, hyperParams);
    if (operationType == AsrPhraseOperationType.UPDATE
        || operationType == AsrPhraseOperationType.DELETE
        || operationType == AsrPhraseOperationType.QUERY) {
      body.addProperty(AsrPhraseApiKeywords.ASR_PHRASE_FINETUNED_OUTPUT, finetunedOutput);
    }
    if (operationType == AsrPhraseOperationType.LIST) {
      if (pageNo != null) {
        body.addProperty(AsrPhraseApiKeywords.ASR_PHRASE_PAGE_NO, pageNo);
      }
      if (pageSize != null) {
        body.addProperty(AsrPhraseApiKeywords.ASR_PHRASE_PAGE_SIZE, pageSize);
      }
    }

    return body;
  }

  @Override
  public Object getInput() {
    throw new UnsupportedOperationException("Unimplemented method 'getInput'");
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {
    if (operationType == null) {
      throw new InputRequiredException("operationType is required");
    }
    if (operationType == AsrPhraseOperationType.CREATE
        || operationType == AsrPhraseOperationType.UPDATE) {
      if (phraseList == null || phraseList.isEmpty()) {
        throw new InputRequiredException(
            "phraseList is required when operationType is CREATE or UPDATE");
      }
    }
    if (operationType == AsrPhraseOperationType.UPDATE
        || operationType == AsrPhraseOperationType.DELETE
        || operationType == AsrPhraseOperationType.QUERY) {
      if (finetunedOutput == null) {
        throw new InputRequiredException(
            "phraseId is required when operationType is UPDATE, DELETE or QUERY");
      }
    }
    if (operationType == AsrPhraseOperationType.LIST) {
      if (pageNo != null && pageNo < 1) {
        throw new InputRequiredException("pageNo should be greater than or equal to 1");
      }
    }
  }
}
