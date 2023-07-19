package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.protocol.Request;
import java.nio.ByteBuffer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class RecognitionStreamParam extends RecognitionBaseParam {

  @NonNull private ByteBuffer audioFrame;

  @Override
  public Request toRequest(Protocol protocol) {
    return Request.builder().binary(audioFrame).build();
  }
}
