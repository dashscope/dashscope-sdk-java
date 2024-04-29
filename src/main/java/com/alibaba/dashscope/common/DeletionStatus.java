package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AssistantDeleted */
@Data
@EqualsAndHashCode(callSuper = true)
public final class DeletionStatus extends FlattenResultBase {

  /**
   * Id
   *
   * <p>(Required)
   */
  @SerializedName("id")
  private String id;
  /**
   * Deleted
   *
   * <p>(Required)
   */
  @SerializedName("deleted")
  private Boolean deleted;
  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object;
}
