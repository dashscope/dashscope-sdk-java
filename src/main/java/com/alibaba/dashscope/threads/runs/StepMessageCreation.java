package com.alibaba.dashscope.threads.runs;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StepMessageCreation extends StepDetailBase {
  @Data
  public class MessageCreation {
    @SerializedName("message_id")
    private String messageId;
  }

  @SerializedName("message_creation")
  private MessageCreation messageCreation;
}
