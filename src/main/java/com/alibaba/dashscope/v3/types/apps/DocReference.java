// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.apps;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One retrieved document behind the answer (RAG citation). */
public final class DocReference {
  @JsonProperty("index_id")
  private String indexId;

  @JsonProperty("doc_id")
  private String docId;

  @JsonProperty("doc_name")
  private String docName;

  @JsonProperty("doc_url")
  private String docUrl;

  @JsonProperty("title")
  private String title;

  @JsonProperty("text")
  private String text;

  @JsonProperty("biz_id")
  private String bizId;

  @JsonProperty("images")
  private List<String> images;

  @JsonProperty("page_number")
  private List<Integer> pageNumber;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  public String getIndexId() {
    return indexId;
  }

  public String getDocId() {
    return docId;
  }

  public String getDocName() {
    return docName;
  }

  public String getDocUrl() {
    return docUrl;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public String getBizId() {
    return bizId;
  }

  public List<String> getImages() {
    return images;
  }

  public List<Integer> getPageNumber() {
    return pageNumber;
  }

  /** Fields this SDK version does not model yet (forward compatibility, design §4). */
  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }
}
