// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.files;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.v3.errors.ApiConnectionException;
import com.alibaba.dashscope.v3.errors.ApiStatusException;
import com.alibaba.dashscope.v3.errors.BadRequestException;
import com.alibaba.dashscope.v3.errors.Errors;
import com.alibaba.dashscope.v3.errors.NotFoundException;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ResponseEnvelope;
import com.alibaba.dashscope.v3.types.files.DeletionStatus;
import com.alibaba.dashscope.v3.types.files.FileList;
import com.alibaba.dashscope.v3.types.files.FileObject;
import com.alibaba.dashscope.v3.types.files.FileUploadParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code files} resource (design §3, Python v2 files.py parity): upload/list/get/delete over
 * {@code /files}. Uploads are multipart posts; {@code fine-tune} purposes are validated as JSONL
 * before a byte goes out.
 */
public final class FilesService {
  static final String BASE_PATH = "/files";
  static final String MODULE = "files";

  // DashScope reports a missing file as a 400-family InvalidParameter whose message ("File not
  // found.") is the only field stating the file is gone; get/delete match it narrowly and
  // re-raise as NotFoundException (Python _check_by_id_response parity). The real status/code
  // are preserved — never an invented 404.
  private static final Pattern FILE_NOT_FOUND =
      Pattern.compile("not\\s+found|not\\s+exist|no\\s+such\\s+file", Pattern.CASE_INSENSITIVE);

  private final HttpTransport transport;

  public FilesService(HttpTransport transport) {
    this.transport = transport;
  }

  public FileObject upload(FileUploadParams params) {
    return upload(params, RequestOptions.NONE);
  }

  public FileObject upload(FileUploadParams params, RequestOptions options) {
    Path file = params.getFile();
    validateUpload(file, params.getPurpose());
    byte[] content;
    try {
      content = Files.readAllBytes(file);
    } catch (IOException e) {
      throw new ApiConnectionException("failed to read upload file " + file, e);
    }
    Map<String, String> fields = new LinkedHashMap<>();
    if (params.getDescription() != null) {
      fields.put("descriptions", params.getDescription());
    }
    ResponseEnvelope envelope =
        transport.executeForm(
            BASE_PATH,
            fields,
            "files",
            file.getFileName().toString(),
            content,
            contentTypeOf(file),
            MODULE,
            options);
    FileObject result = parseUpload(envelope.getBody());
    fillEnvelope(result, envelope);
    return result;
  }

  public FileList list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, RequestOptions.NONE);
  }

  public FileList list(int pageNo, int pageSize, RequestOptions options) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("page_no", String.valueOf(pageNo));
    query.put("page_size", String.valueOf(pageSize));
    ResponseEnvelope envelope = transport.executeGet(BASE_PATH, query, MODULE, options);
    FileList result = parseList(envelope.getBody());
    fillEnvelope(result, envelope);
    return result;
  }

  public FileObject get(String fileId) {
    return get(fileId, RequestOptions.NONE);
  }

  public FileObject get(String fileId, RequestOptions options) {
    requireFileId(fileId);
    ResponseEnvelope envelope = getById(fileId, options);
    FileObject result = parseFile(envelope.getBody());
    fillEnvelope(result, envelope);
    return result;
  }

  public DeletionStatus delete(String fileId) {
    return delete(fileId, RequestOptions.NONE);
  }

  public DeletionStatus delete(String fileId, RequestOptions options) {
    requireFileId(fileId);
    ResponseEnvelope envelope;
    try {
      envelope = transport.executeDelete(BASE_PATH + "/" + fileId, MODULE, options);
    } catch (BadRequestException e) {
      throw refineNotFound(e);
    }
    boolean deleted = true;
    JsonNode data = envelope.getBody() == null ? null : envelope.getBody().get("data");
    if (data != null && data.isObject() && data.has("deleted")) {
      deleted = data.get("deleted").asBoolean();
    }
    DeletionStatus result = new DeletionStatus(deleted);
    fillEnvelope(result, envelope);
    return result;
  }

  private ResponseEnvelope getById(String fileId, RequestOptions options) {
    try {
      return transport.executeGet(BASE_PATH + "/" + fileId, null, MODULE, options);
    } catch (BadRequestException e) {
      throw refineNotFound(e);
    }
  }

  private static ApiStatusException refineNotFound(BadRequestException e) {
    String message = e.getMessage();
    if (message != null && FILE_NOT_FOUND.matcher(message).find()) {
      return new NotFoundException(
          message, e.getCode(), e.getStatusCode(), e.getRequestId(), e.getBody(), e.getHeaders());
    }
    return e;
  }

  private static void requireFileId(String fileId) {
    // An empty id would collapse the path to "/files/", silently hitting the list endpoint
    // (or a confusing gateway error); fail locally.
    if (fileId == null || fileId.trim().isEmpty()) {
      throw new IllegalArgumentException("file_id must be a non-empty string");
    }
  }

  private static void validateUpload(Path file, String purpose) {
    if (!Files.isRegularFile(file)) {
      throw invalidFile("file not found: " + file);
    }
    if (FileUploadParams.PURPOSE_FINE_TUNE.equals(purpose) || "fine_tune".equals(purpose)) {
      if (!isValidJsonl(file)) {
        throw invalidFile("file " + file + " is not in valid jsonl format");
      }
    }
  }

  private static boolean isValidJsonl(Path file) {
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        JsonSupport.readTree(line);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static ApiStatusException invalidFile(String detail) {
    PublicErrorCode entry = PublicErrorCode.INVALID_FILE;
    return Errors.fromStatus(entry.getStatusCode(), entry.getErrorCode(), detail, null, null, null);
  }

  private static FileObject parseUpload(JsonNode body) {
    JsonNode data = body == null ? null : body.get("data");
    if (data == null || !data.isObject()) {
      return new FileObject();
    }
    JsonNode uploaded = data.get("uploaded_files");
    if (uploaded != null && uploaded.isArray()) {
      // The endpoint always answers with a list even though one file went up.
      data = uploaded.isEmpty() ? null : uploaded.get(0);
    }
    return data == null ? new FileObject() : JsonSupport.mapper().convertValue(data, FileObject.class);
  }

  private static FileList parseList(JsonNode body) {
    JsonNode data = body == null ? null : body.get("data");
    if (data != null && data.isObject()) {
      List<FileObject> files = convertFiles(data.get("files"));
      return new FileList(
          intOr(data.get("page_no"), 1),
          intOr(data.get("page_size"), 10),
          intOr(data.get("total"), files.size()),
          files);
    }
    if (data != null && data.isArray()) {
      List<FileObject> files = convertFiles(data);
      return new FileList(1, 10, files.size(), files);
    }
    return new FileList(1, 10, 0, null);
  }

  private static FileObject parseFile(JsonNode body) {
    JsonNode data = body == null ? null : body.get("data");
    if (data == null || !data.isObject()) {
      return new FileObject();
    }
    return JsonSupport.mapper().convertValue(data, FileObject.class);
  }

  private static List<FileObject> convertFiles(JsonNode array) {
    List<FileObject> files = new ArrayList<>();
    if (array != null && array.isArray()) {
      for (JsonNode item : array) {
        files.add(JsonSupport.mapper().convertValue(item, FileObject.class));
      }
    }
    return files;
  }

  private static int intOr(JsonNode node, int fallback) {
    return node != null && node.isNumber() ? node.asInt() : fallback;
  }

  private static String contentTypeOf(Path file) {
    try {
      String probed = Files.probeContentType(file);
      if (probed != null) {
        return probed;
      }
    } catch (IOException e) {
      // fall through to the default
    }
    return "application/octet-stream";
  }

  private static void fillEnvelope(
      com.alibaba.dashscope.v3.types.common.ResponseBase result, ResponseEnvelope envelope) {
    result.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
  }
}
