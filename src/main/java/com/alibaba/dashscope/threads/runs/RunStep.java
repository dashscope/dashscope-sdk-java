package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** RunStep */
@Data
@EqualsAndHashCode(callSuper = true)
public class RunStep extends FlattenResultBase {

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
  private String object;
  /**
   * Created At
   *
   * <p>(Required)
   */
  @SerializedName("created_at")
  private Long createdAt;
  /**
   * Assistant Id
   *
   * <p>(Required)
   */
  @SerializedName("assistant_id")
  private String assistantId;
  /**
   * Thread Id
   *
   * <p>(Required)
   */
  @SerializedName("thread_id")
  private String threadId;
  /**
   * Run Id
   *
   * <p>(Required)
   */
  @SerializedName("run_id")
  private String runId;
  /**
   * Type
   *
   * <p>(Required)
   */
  @SerializedName("type")
  private RunStep.Type type;
  /**
   * Status
   *
   * <p>(Required)
   */
  @SerializedName("status")
  private RunStep.Status status;
  /**
   * Step Details
   *
   * <p>(Required)
   */
  @SerializedName("step_details")
  private StepDetailBase stepDetails;

  @SerializedName("last_error")
  private LastError lastError = null;
  /** Expired At */
  @SerializedName("expired_at")
  private Integer expiredAt = null;

  /** Cancelled At */
  @SerializedName("cancelled_at")
  private Integer cancelledAt = null;
  /** Failed At */
  @SerializedName("failed_at")
  private Integer failedAt = null;
  /** Completed At */
  @SerializedName("completed_at")
  private Integer completedAt = null;
  /** Metadata */
  @SerializedName("metadata")
  private Map<String, String> metadata = null;

  @SerializedName("usage")
  private Usage usage = null;

  /** Status */
  public enum Status {
    @SerializedName("in_progress")
    IN_PROGRESS("in_progress"),
    @SerializedName("cancelled")
    CANCELLED("cancelled"),
    @SerializedName("failed")
    FAILED("failed"),
    @SerializedName("completed")
    COMPLETED("completed"),
    @SerializedName("expired")
    EXPIRED("expired");
    private final String value;
    private static final Map<String, RunStep.Status> CONSTANTS =
        new HashMap<String, RunStep.Status>();

    static {
      for (RunStep.Status c : values()) {
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

    public static RunStep.Status fromValue(String value) {
      RunStep.Status constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }
  }

  /** Type */
  public enum Type {
    @SerializedName("message_creation")
    MESSAGE_CREATION("message_creation"),
    @SerializedName("tool_calls")
    TOOL_CALLS("tool_calls");
    private final String value;
    private static final Map<String, RunStep.Type> CONSTANTS = new HashMap<String, RunStep.Type>();

    static {
      for (RunStep.Type c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    Type(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

    public String value() {
      return this.value;
    }

    public static RunStep.Type fromValue(String value) {
      RunStep.Type constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }
  }
}
