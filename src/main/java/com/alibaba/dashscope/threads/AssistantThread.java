package com.alibaba.dashscope.threads;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AssistantThread */
@Data
@EqualsAndHashCode(callSuper = true)
public final class AssistantThread extends FlattenResultBase {

  /**
   * Id
   *
   * <p>(Required)
   */
  @SerializedName("id")
  private String id;
  /**
   * Created At
   *
   * <p>(Required)
   */
  @SerializedName("created_at")
  private Long createdAt;
  /** Metadata */
  @SerializedName("metadata")
  private Map<String, String> metadata;
}
