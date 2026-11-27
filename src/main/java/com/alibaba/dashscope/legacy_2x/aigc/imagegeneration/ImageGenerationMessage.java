package com.alibaba.dashscope.legacy_2x.aigc.imagegeneration;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@Deprecated
public class ImageGenerationMessage {

  /** The role, can be `user` and `bot`. */
  private String role;

  /** The conversation content. */
  private List<Map<String, Object>> content;
}
