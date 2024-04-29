package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.threads.runs.RunParam.TruncationStrategy;
import com.alibaba.dashscope.tools.ToolBase;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

/** Run */
@Data
@EqualsAndHashCode(callSuper = true)
public class Run extends FlattenResultBase {

  /** (Required) */
  @SerializedName("id")
  private String id;
  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object;
  /**
   * Created At
   *
   * <p>(Required)
   */
  @SerializedName("created_at")
  private Long createdAt;
  /**
   * Thread Id
   *
   * <p>(Required)
   */
  @SerializedName("thread_id")
  private String threadId;
  /**
   * Assistant Id
   *
   * <p>(Required)
   */
  @SerializedName("assistant_id")
  private String assistantId;
  /**
   * Status
   *
   * <p>(Required)
   */
  @SerializedName("status")
  private Run.Status status;

  @SerializedName("required_action")
  private RequiredAction requiredAction = null;

  @SerializedName("last_error")
  private LastError lastError = null;
  /** Expires At */
  @SerializedName("expires_at")
  private Integer expiresAt = null;

  /** Started At */
  @SerializedName("started_at")
  private Integer startedAt = null;

  /** Cancelled At */
  @SerializedName("cancelled_at")
  private Integer cancelledAt = null;
  /** Failed At */
  @SerializedName("failed_at")
  private Integer failedAt = null;

  /** completed At */
  @SerializedName("completed_at")
  private Integer completedAt = null;

  @Data
  public class IncompleteDetails {
    private String reason;
  }

  @SerializedName("incomplete_details")
  private IncompleteDetails incompleteDetails;

  /**
   * Model
   *
   * <p>(Required)
   */
  @SerializedName("model")
  private String model;
  /**
   * Instructions
   *
   * <p>(Required)
   */
  @SerializedName("instructions")
  private String instructions;

  @Singular private List<ToolBase> tools;
  /**
   * File Ids
   *
   * <p>(Required)
   */
  @SerializedName("file_ids")
  private List<String> fileIds = new ArrayList<String>();

  /** Metadata */
  @SerializedName("metadata")
  private Map<String, String> metadata = null;

  @SerializedName("usage")
  private Usage usage = null;
  /** Temperature */
  @SerializedName("temperature")
  private Float temperature = null;

  @SerializedName("max_prompt_tokens")
  private Integer maxPromptTokens;

  @SerializedName("max_completion_tokens")
  private Integer maxCompletionTokens;

  @SerializedName("truncation_strategy")
  private TruncationStrategy truncationStrategy;

  @SerializedName("tool_choice")
  private Object toolChoice;

  /** only support json_object. */
  @SerializedName("response_format")
  private Object responseFormat = "json_object";

  /** Status */
  public enum Status {
    @SerializedName("queued")
    queued("queued"),
    @SerializedName("in_progress")
    IN_PROGRESS("in_progress"),
    @SerializedName("requires_action")
    REQUIRES_ACTION("requires_action"),
    @SerializedName("cancelling")
    CANCELLING("cancelling"),
    @SerializedName("cancelled")
    CANCELLED("cancelled"),
    @SerializedName("failed")
    FAILED("failed"),
    @SerializedName("completed")
    COMPLETED("completed"),
    @SerializedName("expired")
    EXPIRED("expired");
    private final String value;
    private static final Map<String, Run.Status> CONSTANTS = new HashMap<String, Run.Status>();

    static {
      for (Run.Status c : values()) {
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

    public static Run.Status fromValue(String value) {
      Run.Status constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }
  }
}
