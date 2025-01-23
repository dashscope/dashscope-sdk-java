// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.generation;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class SearchInfo {
  /** 联网搜索到的结果。 */
  @SerializedName("search_results")
  private List<SearchResult> searchResults;

  @SuperBuilder
  @Data
  public static class SearchResult {
    /** 搜索结果来源的网站名称。 */
    @SerializedName("site_name")
    private String siteName;

    /** 来源网站的图标URL，如果没有图标则为空字符串。 */
    private String icon;

    /** 搜索结果的序号，表示该搜索结果在search_results中的索引。 */
    private Integer index;

    /** 搜索结果的标题。 */
    private String title;

    /** 搜索结果的链接地址。 */
    private String url;
  }
}
