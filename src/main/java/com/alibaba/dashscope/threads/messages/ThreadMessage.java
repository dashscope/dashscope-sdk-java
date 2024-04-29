package com.alibaba.dashscope.threads.messages;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.threads.ContentBase;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Message */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ThreadMessage extends FlattenResultBase {

  /**
   * Id
   *
   * <p>(Required)
   */
  @SerializedName("id")
  private String id;

  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object = "thread.message";
  /** Completed At */
  @SerializedName("created_at")
  private Long createdAt = null;
  /**
   * Thread Id
   *
   * <p>(Required)
   */
  @SerializedName("thread_id")
  private String threadId;
  /**
   * Status
   *
   * <p>(Required)
   */
  @SerializedName("status")
  private ThreadMessage.Status status;

  @SerializedName("incomplete_details")
  private Object incompleteDetails = null;

  @SerializedName("completed_at")
  private Long completedAt;

  @SerializedName("incomplete_at")
  private Long incompleteAt;

  /**
   * Role
   *
   * <p>(Required)
   */
  @SerializedName("role")
  private Role role;

  private List<ContentBase> content;

  /** Assistant Id */
  @SerializedName("assistant_id")
  private String assistantId = null;

  /** Run Id */
  @SerializedName("run_id")
  private String runId = null;
  /**
   * File Ids
   *
   * <p>(Required)
   */
  @SerializedName("file_ids")
  private List<String> fileIds;

  /** Metadata */
  @SerializedName("metadata")
  private Map<String, String> metadata;

  /** Status */
  public enum Status {
    @SerializedName("in_progress")
    IN_PROGRESS("in_progress"),
    @SerializedName("incomplete")
    INCOMPLETE("incomplete"),
    @SerializedName("completed")
    COMPLETED("completed");
    private final String value;
    private static final Map<String, ThreadMessage.Status> CONSTANTS =
        new HashMap<String, ThreadMessage.Status>();

    static {
      for (ThreadMessage.Status c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    Status(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

    public String value() {
      return this.value;
    }

    public static ThreadMessage.Status fromValue(String value) {
      ThreadMessage.Status constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }
  }
}
