package com.alibaba.dashscope.utils;

import lombok.Data;

/**
 * Result of file upload containing OSS URL and certificate.
 */
@Data
public class UploadResult {
  private String ossUrl;
  private OSSUploadCertificate certificate;

  /**
   * Create upload result.
   *
   * @param ossUrl OSS URL of uploaded file
   * @param certificate Upload certificate used
   */
  public UploadResult(String ossUrl, OSSUploadCertificate certificate) {
    this.ossUrl = ossUrl;
    this.certificate = certificate;
  }
}

