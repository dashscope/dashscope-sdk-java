// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.oss;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OSS upload for local multimodal files (design J8, ported from 2.x OSSUtils): two-step flow —
 * {@code GET /uploads?action=getPolicy&model=} fetches a short-lived policy certificate, then a
 * multipart POST to the returned {@code upload_host} stores the file. The resulting reference is
 * {@code oss://<upload_dir>/<fileName>}. Credentials ride on the 3.0 transport (auth
 * interceptor); failures raise the 3.0 error family.
 */
public final class OssUploader {
  private static final String POLICY_PATH = "/uploads";

  private final HttpTransport transport;

  public OssUploader(HttpTransport transport) {
    this.transport = transport;
  }

  /** Upload one local file, fetching a fresh policy certificate. */
  public String upload(String model, Path file, RequestOptions options) {
    return uploadWithCertificate(certificate(model, options), file);
  }

  /**
   * Fetch the policy certificate; public so a caller uploading several files for one request
   * fetches it once and reuses it (2.x parity — validity is governed server-side).
   */
  public Certificate certificate(String model, RequestOptions options) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("action", "getPolicy");
    query.put("model", model);
    ResponseEnvelope envelope = transport.executeGet(POLICY_PATH, query, "utils", options);
    JsonNode data = envelope.getBody() == null ? null : envelope.getBody().get("data");
    if (data == null || !data.isObject()) {
      PublicErrorCode entry = PublicErrorCode.RESPONSE_ERROR;
      throw Errors.fromStatus(
          entry.getStatusCode(),
          entry.getErrorCode(),
          "uploads policy response carries no data object",
          JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
          JsonSupport.toPlainValue(envelope.getBody()),
          envelope.getHeaders());
    }
    return new Certificate(
        textOrNull(data.get("upload_host")),
        textOrNull(data.get("oss_access_key_id")),
        textOrNull(data.get("signature")),
        textOrNull(data.get("policy")),
        textOrNull(data.get("upload_dir")),
        textOrNull(data.get("x_oss_object_acl")),
        textOrNull(data.get("x_oss_forbid_overwrite")));
  }

  /** Multipart POST the file to the certificate's upload host; returns the oss:// reference. */
  public String uploadWithCertificate(Certificate cert, Path file) {
    String fileName = file.getFileName().toString();
    String key = cert.uploadDir + "/" + fileName;
    MediaType mediaType = MediaType.get(contentTypeOf(file));
    byte[] bytes;
    try {
      bytes = Files.readAllBytes(file);
    } catch (IOException e) {
      throw new ApiConnectionException("failed to read upload file " + file, e);
    }
    // Field order matches the 2.x form exactly (policy/signature before key, file last).
    RequestBody body =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("OSSAccessKeyId", cert.ossAccessKeyId)
            .addFormDataPart("Signature", cert.signature)
            .addFormDataPart("policy", cert.policy)
            .addFormDataPart("key", key)
            .addFormDataPart("x-oss-object-acl", cert.objectAcl)
            .addFormDataPart("x-oss-forbid-overwrite", cert.forbidOverwrite)
            .addFormDataPart("success_action_status", "200")
            .addFormDataPart("x-oss-content-type", mediaType.toString())
            .addFormDataPart("file", fileName, RequestBody.create(bytes, mediaType))
            .build();
    Request request = new Request.Builder().url(cert.uploadHost).post(body).build();
    try (Response response = transport.okhttp().newCall(request).execute()) {
      if (response.isSuccessful()) {
        return "oss://" + key;
      }
      String text = response.body() == null ? "" : response.body().string();
      JsonNode errorBody = JsonSupport.parseErrorBody(text);
      throw Errors.fromStatus(
          response.code(),
          JsonSupport.errorCode(errorBody),
          JsonSupport.errorMessage(errorBody) != null
              ? JsonSupport.errorMessage(errorBody)
              : "oss upload failed: HTTP " + response.code(),
          JsonSupport.requestId(errorBody, null),
          JsonSupport.toPlainValue(errorBody),
          null);
    } catch (IOException e) {
      throw new ApiConnectionException("oss upload failed: " + e.getMessage(), e);
    }
  }

  /** Files.probeContentType with the 2.x extension fallback table. */
  private static String contentTypeOf(Path file) {
    try {
      String probed = Files.probeContentType(file);
      if (probed != null) {
        return probed;
      }
    } catch (IOException e) {
      // fall through to the extension table
    }
    String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
    if (name.endsWith(".mp3")) {
      return "audio/mp3";
    }
    if (name.endsWith(".flac")) {
      return "audio/flac";
    }
    if (name.endsWith(".wav")) {
      return "audio/wav";
    }
    if (name.endsWith(".m4a")) {
      return "audio/mp4";
    }
    for (String ext : new String[] {".png", ".jpeg", ".jpg", ".bmp", ".gif", ".tiff"}) {
      if (name.endsWith(ext)) {
        return "image/" + ext.substring(1);
      }
    }
    return "application/octet-stream";
  }

  private static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }

  /** Server-issued OSS policy certificate (fields named exactly as on the wire). */
  public static final class Certificate {
    private final String uploadHost;
    private final String ossAccessKeyId;
    private final String signature;
    private final String policy;
    private final String uploadDir;
    private final String objectAcl;
    private final String forbidOverwrite;

    private Certificate(
        String uploadHost,
        String ossAccessKeyId,
        String signature,
        String policy,
        String uploadDir,
        String objectAcl,
        String forbidOverwrite) {
      this.uploadHost = uploadHost;
      this.ossAccessKeyId = ossAccessKeyId;
      this.signature = signature;
      this.policy = policy;
      this.uploadDir = uploadDir;
      this.objectAcl = objectAcl;
      this.forbidOverwrite = forbidOverwrite;
    }

    public String getUploadHost() {
      return uploadHost;
    }

    public String getUploadDir() {
      return uploadDir;
    }
  }
}
