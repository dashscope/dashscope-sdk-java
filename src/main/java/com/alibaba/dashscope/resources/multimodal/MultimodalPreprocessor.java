// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.multimodal;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.resources.oss.OssUploader;
import com.alibaba.dashscope.types.multimodal.ContentPart;
import com.alibaba.dashscope.types.multimodal.MultimodalMessage;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Local-file preprocessing (2.x PreprocessMessageInput parity): non-text content values that are
 * {@code file://} URIs or existing local paths are uploaded via {@link OssUploader} and replaced
 * by their {@code oss://} URL; existing {@code oss://} values pass through. Any upload or oss
 * presence sets the {@code X-DashScope-OssResourceResolve: enable} request header. Chat
 * preprocesses {@code user} messages only; embeddings preprocess all contents. The policy
 * certificate is fetched lazily once per call and reused across uploads.
 */
final class MultimodalPreprocessor {
  static final String OSS_RESOLVE_HEADER = "X-DashScope-OssResourceResolve";
  private static final String FILE_SCHEMA = "file://";

  private MultimodalPreprocessor() {}

  static final class ChatResult {
    final List<MultimodalMessage> messages;
    final boolean ossResolve;

    ChatResult(List<MultimodalMessage> messages, boolean ossResolve) {
      this.messages = messages;
      this.ossResolve = ossResolve;
    }
  }

  static final class ContentsResult {
    final List<ContentPart> contents;
    final boolean ossResolve;

    ContentsResult(List<ContentPart> contents, boolean ossResolve) {
      this.contents = contents;
      this.ossResolve = ossResolve;
    }
  }

  static ChatResult preprocessChat(
      String model, List<MultimodalMessage> messages, OssUploader uploader, RequestOptions options) {
    Ctx ctx = new Ctx(model, uploader, options);
    List<MultimodalMessage> out = new ArrayList<>(messages.size());
    for (MultimodalMessage message : messages) {
      if (message != null && "user".equals(message.getRole()) && message.getContent() != null) {
        out.add(
            message.toBuilder().content(processParts(ctx, message.getContent())).build());
      } else {
        out.add(message);
      }
    }
    return new ChatResult(out, ctx.ossResolve);
  }

  static ContentsResult preprocessContents(
      String model, List<ContentPart> contents, OssUploader uploader, RequestOptions options) {
    Ctx ctx = new Ctx(model, uploader, options);
    return new ContentsResult(processParts(ctx, contents), ctx.ossResolve);
  }

  private static List<ContentPart> processParts(Ctx ctx, List<ContentPart> parts) {
    List<ContentPart> out = new ArrayList<>(parts.size());
    for (ContentPart part : parts) {
      ContentPart current = part;
      if (part != null) {
        for (Map.Entry<String, Object> entry : part.asMap().entrySet()) {
          Object newValue = processValue(ctx, entry.getKey(), entry.getValue());
          if (newValue != entry.getValue()) {
            current = current.withValue(entry.getKey(), newValue);
          }
        }
      }
      out.add(current);
    }
    return out;
  }

  private static Object processValue(Ctx ctx, String key, Object value) {
    if (value instanceof String) {
      return processString(ctx, key, (String) value);
    }
    if (value instanceof List) {
      List<?> list = (List<?>) value;
      List<Object> out = new ArrayList<>(list.size());
      boolean changed = false;
      for (Object element : list) {
        Object processed = element instanceof String ? processString(ctx, key, (String) element) : element;
        changed |= processed != element;
        out.add(processed);
      }
      return changed ? out : value;
    }
    return value;
  }

  private static String processString(Ctx ctx, String key, String value) {
    if (!"text".equals(key) && value.startsWith("oss://")) {
      ctx.ossResolve = true;
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
      ctx.ossResolve = true;
      return upload(ctx, f);
    }
    if (!"text".equals(key) && !value.startsWith("http") && isValidPath(value)) {
      File f = new File(value);
      if (f.exists()) {
        ctx.ossResolve = true;
        return upload(ctx, f);
      }
    }
    return value;
  }

  private static String upload(Ctx ctx, File f) {
    if (ctx.certificate == null) {
      ctx.certificate = ctx.uploader.certificate(ctx.model, ctx.options);
    }
    return ctx.uploader.uploadWithCertificate(ctx.certificate, f.toPath());
  }

  private static boolean isValidPath(String pathString) {
    try {
      Paths.get(pathString);
      return true;
    } catch (InvalidPathException e) {
      return false;
    }
  }

  private static com.alibaba.dashscope.errors.ApiStatusException invalidFile(String detail) {
    PublicErrorCode entry = PublicErrorCode.INVALID_FILE;
    return Errors.fromStatus(entry.getStatusCode(), entry.getErrorCode(), detail, null, null, null);
  }

  private static final class Ctx {
    final String model;
    final OssUploader uploader;
    final RequestOptions options;
    OssUploader.Certificate certificate;
    boolean ossResolve;

    Ctx(String model, OssUploader uploader, RequestOptions options) {
      this.model = model;
      this.uploader = uploader;
      this.options = options;
    }
  }
}
