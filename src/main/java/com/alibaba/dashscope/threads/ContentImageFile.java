package com.alibaba.dashscope.threads;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Thread */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ContentImageFile extends ContentBase {
  static {
    registerContent("image_file", ContentImageFile.class);
  }

  private String type = "image_file";

  @Data
  public class ImageFile {
    @SerializedName("file_id")
    private String fileId;
  }
  /**
   * Id
   *
   * <p>(Required)
   */
  @SerializedName("image_file")
  private ImageFile imageFile;
}
