package com.alibaba.dashscope.assistants;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @deprecated The Assistants API (com.alibaba.dashscope.assistants) is deprecated and will be
 *     removed in a future release. Please migrate to the Responses API. See
 *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration details.
 */
@Deprecated
@Data
@EqualsAndHashCode(callSuper = true)
public class AssistantFile extends FlattenResultBase {

  /**
   * Id
   *
   * <p>(Required)
   */
  @SerializedName("id")
  private String id;
  /**
   * Assistant Id
   *
   * <p>(Required)
   */
  @SerializedName("assistant_id")
  private String assistantId;
  /**
   * Created At
   *
   * <p>(Required)
   */
  @SerializedName("created_at")
  private Long createdAt;
  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object;
}
