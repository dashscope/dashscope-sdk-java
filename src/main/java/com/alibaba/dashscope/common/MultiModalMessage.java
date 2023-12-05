// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class MultiModalMessage {

  /** The role, can be `user` and `bot`. */
  private String role;

  /** The conversation content. */
  private List<Map<String, Object>> content;
}
