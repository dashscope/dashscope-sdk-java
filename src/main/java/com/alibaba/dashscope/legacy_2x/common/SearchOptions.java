// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.common;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class SearchOptions {
  /** Whether to include search results in the response. */
  @SerializedName("enable_source")
  @Builder.Default
  private Boolean enableSource = false;

  /**
   * Whether to enable citations in [1] or [ref_1] format. Takes effect when enable_source is true.
   */
  @SerializedName("enable_citation")
  @Builder.Default
  private Boolean enableCitation = false;

  /**
   * Citation format. Takes effect when enable_citation is true. [<number>]: citations in the form
   * [1]; [ref_<number>]: citations in the form [ref_1]. Default: [<number>].
   */
  @SerializedName("citation_format")
  private String citationFormat;

  /** Whether to force search. */
  @SerializedName("forced_search")
  @Builder.Default
  private Boolean forcedSearch = false;

  /**
   * Amount of Internet information to search. standard: retrieve 5 items per request; pro: retrieve
   * 10 items per request. Default: standard.
   */
  @SerializedName("search_strategy")
  private String searchStrategy;

  /**
   * Whether the first data packet in streaming output contains only search source information. Only
   * effective when enable_source is true and in streaming mode. Default is false.
   */
  @SerializedName("prepend_search_result")
  @Builder.Default
  private Boolean prependSearchResult = null;
}
