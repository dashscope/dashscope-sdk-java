// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.oss;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.errors.ApiStatusException;
import com.alibaba.dashscope.v3.errors.Errors;
import com.alibaba.dashscope.v3.errors.PublicErrorCode;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursive media upload walk (Python v2 {@code upload_media_value} parity): strings, lists and
 * maps are walked; every string that is a {@code file://} URI or an existing local path is uploaded
 * via {@link OssUploader} and replaced by its {@code oss://} URL; existing {@code oss://} strings
 * pass through and also flag the resolve header. Any upload or oss presence sets {@link
 * #isOssResolve()}, and the caller then sends {@code X-DashScope-OssResourceResolve: enable}. The
 * policy certificate is fetched lazily once per walk and reused.
 */
public final class OssMediaUpload {
  public static final String RESOLVE_HEADER = "X-DashScope-OssResourceResolve";
  public static final String RESOLVE_ENABLE = "enable";

  private static final String FILE_SCHEMA = "file://";

  private final OssUploader uploader;
  private final String model;
  private final RequestOptions options;
  private OssUploader.Certificate certificate;
  private boolean ossResolve;

  public OssMediaUpload(OssUploader uploader, String model, RequestOptions options) {
    this.uploader = uploader;
    this.model = model;
    this.options = options;
  }

  /** Walk one value; returns the transformed value (identity when nothing needed uploading). */
  public Object process(Object value) {
    if (value instanceof String) {
      return processString((String) value);
    }
    if (value instanceof List) {
      List<?> list = (List<?>) value;
      List<Object> out = new ArrayList<>(list.size());
      boolean changed = false;
      for (Object element : list) {
        Object processed = process(element);
        changed |= processed != element;
        out.add(processed);
      }
      return changed ? out : value;
    }
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      Map<Object, Object> out = new LinkedHashMap<>();
      boolean changed = false;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object processed = process(entry.getValue());
        changed |= processed != entry.getValue();
        out.put(entry.getKey(), processed);
      }
      return changed ? out : value;
    }
    return value;
  }

  /** true when any string was an oss:// reference or was uploaded. */
  public boolean isOssResolve() {
    return ossResolve;
  }

  private String processString(String value) {
    if (value.startsWith("oss://")) {
      ossResolve = true;
      return value;
    }
    if (value.startsWith(FILE_SCHEMA)) {
      File f;
      try {
        f = new File(new URI(value));
      } catch (URISyntaxException e) {
        throw invalidFile("invalid file URI " + value + ": " + e.getMessage());
      }
      if (!f.exists()) {
        throw invalidFile("local file not exists: " + value);
      }
      ossResolve = true;
      return upload(f);
    }
    if (!value.startsWith("http") && isValidPath(value)) {
      File f = new File(value);
      if (f.exists()) {
        ossResolve = true;
        return upload(f);
      }
    }
    return value;
  }

  private String upload(File f) {
    if (certificate == null) {
      certificate = uploader.certificate(model, options);
    }
    return uploader.uploadWithCertificate(certificate, f.toPath());
  }

  private static boolean isValidPath(String pathString) {
    try {
      Paths.get(pathString);
      return true;
    } catch (InvalidPathException e) {
      return false;
    }
  }

  private static ApiStatusException invalidFile(String detail) {
    PublicErrorCode entry = PublicErrorCode.INVALID_FILE;
    return Errors.fromStatus(entry.getStatusCode(), entry.getErrorCode(), detail, null, null, null);
  }
}
