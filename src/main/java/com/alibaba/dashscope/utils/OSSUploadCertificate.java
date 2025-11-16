package com.alibaba.dashscope.utils;

import lombok.Data;

/**
 * OSS upload certificate for reuse across multiple file uploads.
 */
@Data
public class OSSUploadCertificate {
  private String uploadHost;
  private String ossAccessKeyId;
  private String signature;
  private String policy;
  private String uploadDir;
  private String xOssObjectAcl;
  private String xOssForbidOverwrite;

  /**
   * Create certificate from upload info data.
   *
   * @param uploadHost OSS upload host
   * @param ossAccessKeyId OSS access key ID
   * @param signature Upload signature
   * @param policy Upload policy
   * @param uploadDir Upload directory
   * @param xOssObjectAcl OSS object ACL
   * @param xOssForbidOverwrite OSS forbid overwrite flag
   */
  public OSSUploadCertificate(String uploadHost, String ossAccessKeyId,
      String signature, String policy, String uploadDir,
      String xOssObjectAcl, String xOssForbidOverwrite) {
    this.uploadHost = uploadHost;
    this.ossAccessKeyId = ossAccessKeyId;
    this.signature = signature;
    this.policy = policy;
    this.uploadDir = uploadDir;
    this.xOssObjectAcl = xOssObjectAcl;
    this.xOssForbidOverwrite = xOssForbidOverwrite;
  }
}

