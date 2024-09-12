package com.alibaba.dashscope.audio.asr.vocabulary;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode
@Data
public class VocabularyParam extends HalfDuplexServiceParam {
  private String prefix;
  private String targetModel;
  private VocabularyOperationType operationType;
  private JsonArray vocabulary;
  private String vocabularyId;

  private int pageIndex;
  private int pageSize;

  protected VocabularyParam(HalfDuplexServiceParamBuilder<?, ?> b) {
    super(b);
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.add(ApiKeywords.INPUT, getInput());
    body.addProperty("model", getModel());
    return body;
  }

  @Override
  public JsonObject getInput() {
    JsonObject input = new JsonObject();
    switch (operationType) {
      case CREATE:
        input.addProperty(ApiKeywords.ACTION, operationType.getValue());
        input.addProperty("target_model", targetModel);
        input.addProperty("prefix", prefix);
        input.add("vocabulary", vocabulary);
        break;
      case LIST:
        input.addProperty(ApiKeywords.ACTION, operationType.getValue());
        if (prefix != null) {
          input.addProperty("prefix", prefix);
        }
        input.addProperty("page_index", pageIndex);
        input.addProperty("page_size", pageSize);
        break;
      case QUERY:
        input.addProperty(ApiKeywords.ACTION, operationType.getValue());
        input.addProperty("vocabulary_id", vocabularyId);
        break;
      case UPDATE:
        input.addProperty(ApiKeywords.ACTION, operationType.getValue());
        input.addProperty("vocabulary_id", vocabularyId);
        input.add("vocabulary", vocabulary);
        break;
      case DELETE:
        input.addProperty(ApiKeywords.ACTION, operationType.getValue());
        input.addProperty("vocabulary_id", vocabularyId);
        break;
      default:
        throw new InvalidParameterException("operationType is not supported, should not be here.");
    }
    return input;
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
    switch (operationType) {
      case CREATE:
        if (vocabulary == null) {
          throw new InputRequiredException("vocabulary is required when Create Vocabulary");
        }
        if (prefix == null) {
          throw new InputRequiredException("prefix is required");
        }
        if (targetModel == null) {
          throw new InputRequiredException("targetModel is required");
        }
        break;
      case LIST:
        if (pageIndex < 0) {
          throw new InputRequiredException("pageIndexs should be greater or equal to 0");
        }
        if (pageSize < 1) {
          throw new InputRequiredException("pageSize should be greater or equal to 1");
        }
        break;
      case QUERY:
        if (vocabularyId == null) {
          throw new InputRequiredException("vocabulary id is required when Query Vocabulary");
        }
        break;
      case UPDATE:
        if (vocabularyId == null) {
          throw new InputRequiredException("vocabulary id is required when Update Vocabulary");
        }
        if (vocabulary == null) {
          throw new InputRequiredException("vocabulary is required when Update Vocabulary");
        }
        break;
      case DELETE:
        if (vocabularyId == null) {
          throw new InputRequiredException("vocabulary id is required when Delete Voice");
        }
        break;
      default:
        throw new InvalidParameterException("operationType is not supported, should not be here.");
    }
  }
}
