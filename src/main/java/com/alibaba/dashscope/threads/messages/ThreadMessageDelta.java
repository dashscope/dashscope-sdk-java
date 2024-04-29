package com.alibaba.dashscope.threads.messages;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.threads.ContentBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** MessageDeltaEvent */
@Data
@EqualsAndHashCode(callSuper = true)
public class ThreadMessageDelta extends FlattenResultBase {

  /**
   * Id
   *
   * <p>(Required)
   */
  @SerializedName("id")
  private String id;
  /**
   * MessageDelta
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

    /** Content */
    @SerializedName("content")
    private List<ContentBase> content = null;
    /** File Ids */
    @SerializedName("file_ids")
    private List<String> fileIds = null;
    /** Role */
    @SerializedName("role")
    private Role role = null;
  }
}
