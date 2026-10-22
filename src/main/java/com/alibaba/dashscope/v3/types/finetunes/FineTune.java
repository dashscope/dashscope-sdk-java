// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.finetunes;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.alibaba.dashscope.v3.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * One fine-tune job (Python v2 parity). The same type comes back from {@code create} / {@code
 * get} and is nested in {@link FineTuneList#getJobs()}, so there is one shape to learn. The
 * server reports a failure {@code code} per job, so the envelope is meaningful in both roles: a
 * failed job in a list flags itself through {@link #isError()} as well.
 */
public final class FineTune extends ResponseBase {
  public static final String SUCCEEDED = "SUCCEEDED";
  public static final String FAILED = "FAILED";
  public static final String CANCELED = "CANCELED";

  @JsonProperty("job_id")
  private String jobId;

  @JsonProperty("job_name")
  private String jobName;

  @JsonProperty("status")
  private String status;

  @JsonProperty("model")
  private String model;

  @JsonProperty("base_model")
  private String baseModel;

  @JsonProperty("finetuned_model")
  @JsonAlias("finetuned_output")
  private String finetunedModel;

  @JsonProperty("training_file_ids")
  private List<String> trainingFileIds;

  @JsonProperty("validation_file_ids")
  private List<String> validationFileIds;

  @JsonProperty("hyper_parameters")
  private Map<String, Object> hyperParameters;

  @JsonProperty("training_type")
  private String trainingType;

  @JsonProperty("created_at")
  @JsonAlias("create_time")
  private String createdAt;

  @JsonProperty("finished_at")
  @JsonAlias("end_time")
  private String finishedAt;

  @JsonProperty("user_identity")
  private String userIdentity;

  @JsonProperty("modifier")
  private String modifier;

  @JsonProperty("creator")
  private String creator;

  @JsonProperty("group")
  private String group;

  @JsonProperty("usage")
  private Usage usage;

  public String getJobId() {
    return jobId;
  }

  public String getJobName() {
    return jobName;
  }

  public String getStatus() {
    return status;
  }

  public String getModel() {
    return model;
  }

  public String getBaseModel() {
    return baseModel;
  }

  public String getFinetunedModel() {
    return finetunedModel;
  }

  public List<String> getTrainingFileIds() {
    return trainingFileIds;
  }

  public List<String> getValidationFileIds() {
    return validationFileIds;
  }

  public Map<String, Object> getHyperParameters() {
    return hyperParameters;
  }

  public String getTrainingType() {
    return trainingType;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public String getFinishedAt() {
    return finishedAt;
  }

  public String getUserIdentity() {
    return userIdentity;
  }

  public String getModifier() {
    return modifier;
  }

  public String getCreator() {
    return creator;
  }

  public String getGroup() {
    return group;
  }

  public Usage getUsage() {
    return usage;
  }

  /** {@code Job} spells the same identifier {@code taskId}. */
  public String getTaskId() {
    return jobId;
  }

  public boolean isSucceeded() {
    return SUCCEEDED.equals(status);
  }

  /**
   * Terminal: SUCCEEDED / FAILED / CANCELED — deliberately narrower than {@code Job.isFinished}
   * (UNKNOWN is not terminal on this plane; Python v2 fine_tunes._TERMINAL parity).
   */
  public boolean isFinished() {
    return SUCCEEDED.equals(status) || FAILED.equals(status) || CANCELED.equals(status);
  }
}
