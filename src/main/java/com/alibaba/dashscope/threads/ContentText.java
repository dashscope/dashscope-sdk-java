package com.alibaba.dashscope.threads;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Thread */
@Data
@EqualsAndHashCode(callSuper = true)
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
