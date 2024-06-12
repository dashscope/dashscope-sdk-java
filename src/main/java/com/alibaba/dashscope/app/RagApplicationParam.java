// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Title Rag application param.<br>
 * Description Rag application param including doc info.<br>
 * Created at 2024-02-23 16:55
 *
 * @since jdk8
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Deprecated
public class RagApplicationParam extends ApplicationParam {
  /** Doc reference type for returning query result of document retrival */
  public static class DocReferenceType {
    public static String SIMPLE = "simple";
    public static String INDEXED = "indexed";
  }

  /** Tag code list for doc retrival */
  private List<String> docTagCodes;

  /**
   * the type of doc reference simple: simple format of doc retrival which not include index in
   * response text but in doc reference list indexed: include both index in response text and doc
   * reference list
   */
  private String docReferenceType;

  @Override
  public JsonObject getInput() {
    JsonObject input = super.getInput();

    input.addProperty(AppKeywords.DOC_REFERENCE_TYPE, docReferenceType);
    if (docTagCodes != null && docTagCodes.size() > 0) {
      input.add(AppKeywords.DOC_TAG_CODES, JsonUtils.toJsonElement(docTagCodes).getAsJsonArray());
    }

    return input;
  }
}
