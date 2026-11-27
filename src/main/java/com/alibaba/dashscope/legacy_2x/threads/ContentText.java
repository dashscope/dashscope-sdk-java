package com.alibaba.dashscope.legacy_2x.threads;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Thread */
@Data
@EqualsAndHashCode(callSuper = true)
@Deprecated
public final class ContentText extends ContentBase {
  static {
    registerContent("text", ContentText.class);
  }

  private String type = "text";

  @Data
  public class Text {
    String value;
    List<AnnotationBase> annotations;
  }

  private Text text;
}
