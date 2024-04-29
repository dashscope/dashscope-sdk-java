package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** RunStepDeltaEvent */
@Data
@EqualsAndHashCode(callSuper = true)
public class RunStepDelta extends FlattenResultBase {

  /**
   * Id
   *
   * <p>(Required)
   */
  @SerializedName("id")
  private String id;
  /**
   * RunStepDelta
   *
   * <p>(Required)
   */
  @SerializedName("delta")
  private Delta delta;
  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object;

  @Data
  public static class Delta {
    @SerializedName("step_details")
    private StepDetailBase stepDetails;
  }
}
