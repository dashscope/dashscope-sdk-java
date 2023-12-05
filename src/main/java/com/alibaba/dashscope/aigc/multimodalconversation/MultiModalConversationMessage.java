// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.multimodalconversation;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@Deprecated
public class MultiModalConversationMessage {
  /** @Deprecated use MultiModalMessage instead. */

  /** The role, can be `user` and `bot`. */
  String role;

  /** The conversation content. */
  List<MultiModalMessageItemBase> content;
}
