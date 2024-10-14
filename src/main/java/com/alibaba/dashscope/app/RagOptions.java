/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.dashscope.app;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Title Rag options for application call params.<br>
 * Description Rag options for application call params.<br>
 * Created at 2024-02-23 16:15
 *
 * @since jdk8
 */
@Data
@SuperBuilder
public class RagOptions {
  /** knowledge base ids */
  @SerializedName("pipeline_ids")
  private List<String> pipelineIds;

  /** file ids of knowledge base */
  @SerializedName("file_ids")
  private List<String> fileIds;

  /** tags of knowledge base */
  @SerializedName("tags")
  private List<String> tags;

  /** metadata filter of knowledge base query */
  @SerializedName("metadata_filter")
  private JsonObject metadataFilter;

  /** structured filter of knowledge base query */
  @SerializedName("structured_filter")
  private JsonObject structuredFilter;
}
