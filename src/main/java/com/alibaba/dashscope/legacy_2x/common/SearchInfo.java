// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.common;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@Deprecated
public class SearchInfo {
  /** Results returned by web search. */
  @SerializedName("search_results")
  private List<SearchResult> searchResults;

  @SuperBuilder
  @Data
  public static class SearchResult {
    /** Name of the website providing the search result. */
    @SerializedName("site_name")
    private String siteName;

    /** Icon URL of the source website, or an empty string if no icon is available. */
    private String icon;

    /** Index of this result in search_results. */
    private Integer index;

    /** Title of the search result. */
    private String title;

    /** URL of the search result. */
    private String url;
  }
}
