package com.alibaba.dashscope.threads;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FilePathAnnotation extends AnnotationBase {
  private Long index = null;

  @Data
  public class FilePath {
    @SerializedName("file_id")
    private String file_id;
  }

  @SerializedName("file_path")
  private FilePath filePath;
}
