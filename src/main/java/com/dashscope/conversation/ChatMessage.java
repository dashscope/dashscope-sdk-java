package com.dashscope.conversation;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class ChatMessage {

  /** The role, can be `user` and `bot`. */
  String role;

  /** The conversation content. */
  String payload;
}
