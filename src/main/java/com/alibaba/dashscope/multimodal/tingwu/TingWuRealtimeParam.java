package com.alibaba.dashscope.multimodal.tingwu;

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import io.reactivex.Flowable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.alibaba.dashscope.multimodal.MultiModalDialogApiKeyWords.CONST_NAME_DIRECTIVE;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class TingWuRealtimeParam extends FullDuplexServiceParam {
  private Integer sampleRate;
  private String format;
  private String terminology;
  private Integer maxEndSilence;
  private String appId;

  private Map<String, Object> input;

  public void clearParameters() {
      input.clear();
  }


  @Override
  public Map<String, String> getHeaders() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Object> getParameters() {
      Map<String, Object> params = new HashMap<>();
      params.put("sampleRate", sampleRate);
      params.put("format", format);
      params.put("terminology", terminology);
      if (maxEndSilence != null) {
          params.put("maxEndSilence", maxEndSilence);
      }
      if (parameters != null) {
          params.putAll(parameters);
      }
      return params;
  }

  @Override
  public Map<String, Object> getInputs() {
      if (input == null) {
          input = new HashMap<>();
      }
      input.put("appId", appId);

      return input;
  }

  public void setDirective(String directive) {
      if (input == null) {
          input = new HashMap<>();
      }
      input.put(CONST_NAME_DIRECTIVE, directive);
  }

  @Override
  public Flowable<Object> getStreamingData() {
    return null;
  }
}
