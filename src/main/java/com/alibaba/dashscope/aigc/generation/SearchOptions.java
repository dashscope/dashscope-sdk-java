// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.generation;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class SearchOptions {
  /** 在返回结果中是否展示搜索到的信息 */
  @SerializedName("enable_source")
  @Builder.Default
  private Boolean enableSource = false;

  /** 是否开启[1]或[ref_1]样式的角标标注功能。在enable_source为true时生效。 */
  @SerializedName("enable_citation")
  @Builder.Default
  private Boolean enableCitation = false;

  /**
   * 角标样式。在enable_citation为true时生效。[<number>]：角标形式为[1], [ref_<number>]：角标形式为[ref_1]。 默认为[<number>]
   */
  @SerializedName("citation_format")
  private String citationFormat;

  /** 是否强制开启搜索。 */
  @SerializedName("forced_search")
  @Builder.Default
  private Boolean forcedSearch = false;

  /** 搜索互联网信息的数量。standard：在请求时搜索5条互联网信息; pro：在请求时搜索10条互联网信息。 默认值为standard */
  @SerializedName("search_strategy")
  private String searchStrategy;

  /**
   * Whether the first data packet in streaming output contains only
   * search source information. Only effective when enable_source is
   * true and in streaming mode. Default is false.
   */
  @SerializedName("prepend_search_result")
  @Builder.Default
  private Boolean prependSearchResult = null;
}
