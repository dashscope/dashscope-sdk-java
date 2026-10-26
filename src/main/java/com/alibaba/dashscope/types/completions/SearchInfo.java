// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.completions;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Web-search side information attached to a completion (enable_search flows). */
public class SearchInfo {
  @JsonProperty("search_results")
  private List<SearchResult> searchResults;

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }

  public static class SearchResult {
    @JsonProperty("index")
    private int index;

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("site_name")
    private String siteName;

    @JsonProperty("icon")
    private String icon;

    public int getIndex() {
      return index;
    }

    public String getTitle() {
      return title;
    }

    public String getUrl() {
      return url;
    }

    public String getSiteName() {
      return siteName;
    }

    public String getIcon() {
      return icon;
    }
  }
}
