package com.alibaba.dashscope.threads.messages;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** MessageFile */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageFile extends FlattenResultBase {

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
  /**
   * Message Id
   *
   * <p>(Required)
   */
  @SerializedName("message_id")
  private String messageId;
  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object = "thread.message.file";
}
