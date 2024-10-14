// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

/**
 * Title App completion keywords.<br>
 * Description App completion keywords.<br>
 * Created at 2024-02-23 16:36
 *
 * @since jdk8
 */
public interface AppKeywords {
  /** http headers */
  String X_DASHSCOPE_WORKSPACE = "X-DashScope-WorkSpace";

  /** input key */
  String SESSION_ID = "session_id";

  String BIZ_PARAMS = "biz_params";

  String DOC_TAG_CODES = "doc_tag_codes";

  String DOC_REFERENCE_TYPE = "doc_reference_type";

  String MEMORY_ID = "memory_id";

  /** parameters key */
  String TOP_P = "top_p";

  String TOP_K = "top_k";

  String SEED = "seed";

  String TEMPERATURE = "temperature";

  String HAS_THOUGHTS = "has_thoughts";

  String IMAGES = "image_list";
}
