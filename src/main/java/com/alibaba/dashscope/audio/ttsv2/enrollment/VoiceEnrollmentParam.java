package com.alibaba.dashscope.audio.ttsv2.enrollment;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode
@Data
public class VoiceEnrollmentParam extends HalfDuplexServiceParam {
  private String prefix;
  private String targetModel;
  private VoiceEnrollmentOperationType operationType;
  private String url;
  private String voiceId;
  private List<String> languageHints = null;

  private int pageIndex;
  private int pageSize;

  protected VoiceEnrollmentParam(HalfDuplexServiceParamBuilder<?, ?> b) {
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
        input.addProperty("url", url);
        if (languageHints != null) {
          input.add("language_hints", JsonUtils.toJsonArray(languageHints));
        }
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
        input.addProperty("voice_id", voiceId);
        break;
      case UPDATE:
        input.addProperty(ApiKeywords.ACTION, operationType.getValue());
        input.addProperty("voice_id", voiceId);
        input.addProperty("url", url);
        break;
      case DELETE:
        input.addProperty(ApiKeywords.ACTION, operationType.getValue());
        input.addProperty("voice_id", voiceId);
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
        if (url == null) {
          throw new InputRequiredException("url is required when Create Voice");
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
        if (voiceId == null) {
          throw new InputRequiredException("voice id is required when Query Voice");
        }
        break;
      case UPDATE:
        if (voiceId == null) {
          throw new InputRequiredException("voice id is required when Update Voice");
        }
        if (url == null) {
          throw new InputRequiredException("url is required when Update Voice");
        }
        break;
      case DELETE:
        if (voiceId == null) {
          throw new InputRequiredException("voice id is required when Delete Voice");
        }
        break;
      default:
        throw new InvalidParameterException("operationType is not supported, should not be here.");
    }
  }
}
