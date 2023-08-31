// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class Message {

  /** The role, can be `user` and `bot`. */
  String role;

  /** The conversation content. */
  String content;
}
