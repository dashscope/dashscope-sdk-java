// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class FullDuplexTestParam extends FullDuplexServiceParam {
  @SerializedName("top_p")
  private Double topP;

  private String str;
  private String text;
  private ByteBuffer binary;
  private Flowable<Object> streamData;

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (topP != null) {
      params.put("top_p", topP);
    }
    if (str != null) {
      params.put("str", str);
    }
    params.putAll(parameters);
    return params;
  }

  @Override
  public Flowable<Object> getStreamingData() {
    return streamData.cast(Object.class);
  }
}
