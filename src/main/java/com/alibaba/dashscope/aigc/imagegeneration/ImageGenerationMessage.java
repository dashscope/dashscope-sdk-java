package com.alibaba.dashscope.aigc.imagegeneration;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class ImageGenerationMessage {

  /** The role, can be `user` and `bot`. */
  private String role;

  /** The conversation content. */
  private List<Map<String, Object>> content;
}
