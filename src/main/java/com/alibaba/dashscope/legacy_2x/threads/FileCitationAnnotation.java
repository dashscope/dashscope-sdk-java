package com.alibaba.dashscope.legacy_2x.threads;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Deprecated
public class FileCitationAnnotation extends AnnotationBase {
  private Long index = null;

  @Data
  public class FileCitation {
    @SerializedName("file_id")
    private String fileId;

    private String quote;
  }

  @SerializedName("file_citation")
  private FileCitation fileCitation;
}
