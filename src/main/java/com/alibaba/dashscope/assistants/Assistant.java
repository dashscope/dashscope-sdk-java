package com.alibaba.dashscope.assistants;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.tools.ToolBase;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Assistant */
@Data
@EqualsAndHashCode(callSuper = true)
public final class Assistant extends FlattenResultBase {

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
  /** Description */
  @SerializedName("description")
  private String description = null;
  /**
   * File Ids
   *
   * <p>(Required)
   */
  @SerializedName("file_ids")
  private List<String> fileIds = new ArrayList<String>();
  /** Instructions */
  @SerializedName("instructions")
  private String instructions = null;
  /** Metadata */
  @SerializedName("metadata")
  private Map<String, String> metadata = null;
  /**
   * Model
   *
   * <p>(Required)
   */
  @SerializedName("model")
  private String model;
  /** Name */
  @SerializedName("name")
  private String name = null;
  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object;
  /**
   * Tools
   *
   * <p>(Required)
   */
  @SerializedName("tools")
  private List<ToolBase> tools;

  /**
   * Top P
   */
  @SerializedName("top_p")
  private Double topP;

  /**
   * Top K
   */
  @SerializedName("top_k")
  private Integer topK;

  /**
   * Temperature
   *
   * <p>(Required)
   */
  @SerializedName("temperature")
  private Double temperature;

  /**
   * Max Tokens
   */
  @SerializedName("max_tokens")
  private Integer maxTokens;
}
