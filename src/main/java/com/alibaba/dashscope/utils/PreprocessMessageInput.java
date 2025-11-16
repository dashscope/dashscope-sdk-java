package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageItemBase;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PreprocessMessageInput {

  public static boolean isValidPath(String pathString) {
    try {
      Paths.get(pathString);
      return true;
    } catch (InvalidPathException e) {
      return false;
    }
  }

  /**
   * Check and upload file with certificate reuse support.
   *
   * @param model Model name
   * @param message Message item containing file path
   * @param apiKey API key
   * @param certificate Optional upload certificate for reuse
   * @return CheckAndUploadResult containing upload status and certificate
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static CheckAndUploadResult checkAndUpload(
      String model, MultiModalMessageItemBase message, String apiKey,
      OSSUploadCertificate certificate)
      throws NoApiKeyException, UploadFileException {
    boolean isUpload = false;
    OSSUploadCertificate cert = certificate;

    if (!message.getModal().equals("text")
        && message.getContent().startsWith(ApiKeywords.FILE_PATH_SCHEMA)) {
      try {
        URI fileURI = new URI(message.getContent());
        File f = new File(fileURI);
        if (f.exists()) {
          UploadResult result = OSSUtils.uploadWithCertificate(model,
              f.getAbsolutePath(), apiKey, cert);
          if (result.getOssUrl() == null) {
            throw new UploadFileException(
                String.format("Uploading file: %s failed",
                    message.getContent()));
          }
          message.setContent(result.getOssUrl());
          cert = result.getCertificate();
          isUpload = true;
        } else {
          throw new UploadFileException(
              String.format("Local file: %s not exists.",
                  message.getContent()));
        }
      } catch (URISyntaxException e) {
        throw new UploadFileException(e.getMessage());
      }
    } else if (!message.getModal().equals("text")
        && message.getContent().startsWith("oss://")) {
      isUpload = true;
    } else if (!message.getModal().equals("text")
        && !message.getContent().startsWith("http")) {
      if (isValidPath(message.getContent())) {
        File f = new File(message.getContent());
        if (f.exists()) {
          UploadResult result = OSSUtils.uploadWithCertificate(model,
              f.getAbsolutePath(), apiKey, cert);
          if (result.getOssUrl() == null) {
            throw new UploadFileException(
                String.format("Uploading file: %s failed",
                    message.getContent()));
          }
          message.setContent(result.getOssUrl());
          cert = result.getCertificate();
          isUpload = true;
        }
      }
    }
    return new CheckAndUploadResult(isUpload, cert);
  }

  /**
   * Preprocess message inputs with certificate reuse support.
   *
   * @param model Model name
   * @param messages List of message items
   * @param apiKey API key
   * @param certificate Optional upload certificate for reuse
   * @return PreprocessResult containing upload status and certificate
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static <T extends MultiModalMessageItemBase> PreprocessResult
      preProcessMessageInputs(String model, List<T> messages, String apiKey,
          OSSUploadCertificate certificate)
      throws NoApiKeyException, UploadFileException {
    boolean hasUpload = false;
    OSSUploadCertificate cert = certificate;

    for (MultiModalMessageItemBase elem : messages) {
      CheckAndUploadResult result = checkAndUpload(model, elem, apiKey,
          cert);
      if (result.isUpload() && !hasUpload) {
        hasUpload = true;
      }
      cert = result.getCertificate();
    }
    return new PreprocessResult(hasUpload, cert);
  }

  /**
   * Preprocess message inputs without certificate reuse (legacy).
   *
   * @param model Model name
   * @param messages List of message items
   * @param apiKey API key
   * @return true if any file was uploaded
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static <T extends MultiModalMessageItemBase> boolean
      preProcessMessageInputs(String model, List<T> messages, String apiKey)
      throws NoApiKeyException, UploadFileException {
    PreprocessResult result = preProcessMessageInputs(model, messages,
        apiKey, null);
    return result.hasUpload();
  }

  /**
   * Check and upload one multimodal message with certificate reuse.
   *
   * @param model Model name
   * @param apiKey API key
   * @param key Message key
   * @param value Message value (file path)
   * @param certificate Optional upload certificate for reuse
   * @return CheckAndUploadOneResult containing file URL and certificate
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static CheckAndUploadOneResult checkAndUploadOneMultiModalMessage(
      String model, String apiKey, String key, String value,
      OSSUploadCertificate certificate)
      throws NoApiKeyException, UploadFileException {
    String dstValue = value;
    OSSUploadCertificate cert = certificate;

    if (value.startsWith(ApiKeywords.FILE_PATH_SCHEMA)) {
      try {
        URI fileURI = new URI(value);
        File f = new File(fileURI);
        if (f.exists()) {
          UploadResult result = OSSUtils.uploadWithCertificate(model,
              f.getAbsolutePath(), apiKey, cert);
          if (result.getOssUrl() == null) {
            throw new UploadFileException(String.format(
                "Uploading file: %s failed", value));
          }
          dstValue = result.getOssUrl();
          cert = result.getCertificate();
        } else {
          throw new UploadFileException(String.format(
              "Local file: %s not exists.", value));
        }
      } catch (URISyntaxException e) {
        throw new UploadFileException(e.getMessage());
      }
    } else if (!key.equals("text") && !value.startsWith("http")) {
      if (isValidPath(value)) {
        File f = new File(value);
        if (f.exists()) {
          UploadResult result = OSSUtils.uploadWithCertificate(model,
              f.getAbsolutePath(), apiKey, cert);
          if (result.getOssUrl() == null) {
            throw new UploadFileException(String.format(
                "Uploading file: %s failed", value));
          }
          dstValue = result.getOssUrl();
          cert = result.getCertificate();
        }
      }
    }

    return new CheckAndUploadOneResult(dstValue, cert);
  }

  /**
   * Check and upload one multimodal message without certificate reuse.
   *
   * @param model Model name
   * @param apiKey API key
   * @param key Message key
   * @param value Message value (file path)
   * @return File URL
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static String checkAndUploadOneMultiModalMessage(
      String model, String apiKey, String key, String value)
      throws NoApiKeyException, UploadFileException {
    CheckAndUploadOneResult result = checkAndUploadOneMultiModalMessage(
        model, apiKey, key, value, null);
    return result.getFileUrl();
  }

  /**
   * Check and upload multimodal message with certificate reuse.
   *
   * @param model Model name
   * @param entry Message entry
   * @param apiKey API key
   * @param certificate Optional upload certificate for reuse
   * @return CheckAndUploadResult containing upload status and certificate
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static CheckAndUploadResult checkAndUploadMultiModalMessage(
      String model, Map.Entry<String, Object> entry, String apiKey,
      OSSUploadCertificate certificate)
      throws NoApiKeyException, UploadFileException {
    boolean isUpload = false;
    OSSUploadCertificate cert = certificate;
    String key = entry.getKey();
    Object value = entry.getValue();

    if (value instanceof List) {
      List<?> dstValue = (List<?>) value;
      for (int i = 0; i < dstValue.size(); i++) {
        Object v = dstValue.get(i);
        if (v instanceof String) {
          if (!key.equals("text") && ((String)v).startsWith("oss://")) {
            isUpload = true;
          } else {
            CheckAndUploadOneResult result =
                checkAndUploadOneMultiModalMessage(model, apiKey, key,
                    (String) v, cert);
            if (!result.getFileUrl().equals(v)) {
              isUpload = true;
              ((List<Object>) dstValue).set(i, result.getFileUrl());
            }
            cert = result.getCertificate();
          }
        }
      }
      entry.setValue(dstValue);
    } else if (value instanceof String) {
      if (!key.equals("text") && ((String)value).startsWith("oss://")) {
        isUpload = true;
      } else {
        CheckAndUploadOneResult result =
            checkAndUploadOneMultiModalMessage(model, apiKey, key,
                (String) value, cert);
        if (!result.getFileUrl().equals(value)) {
          isUpload = true;
          entry.setValue(result.getFileUrl());
        }
        cert = result.getCertificate();
      }
    }
    return new CheckAndUploadResult(isUpload, cert);
  }

  /**
   * Check and upload multimodal message without certificate reuse.
   *
   * @param model Model name
   * @param entry Message entry
   * @param apiKey API key
   * @return true if any file was uploaded
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static boolean checkAndUploadMultiModalMessage(
      String model, Map.Entry<String, Object> entry, String apiKey)
      throws NoApiKeyException, UploadFileException {
    CheckAndUploadResult result = checkAndUploadMultiModalMessage(model,
        entry, apiKey, null);
    return result.isUpload();
  }

  /**
   * Preprocess multimodal message inputs with certificate reuse.
   *
   * @param model Model name
   * @param messages Multimodal message
   * @param apiKey API key
   * @param certificate Optional upload certificate for reuse
   * @return PreprocessResult containing upload status and certificate
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static PreprocessResult preProcessMultiModalMessageInputs(
      String model, MultiModalMessage messages, String apiKey,
      OSSUploadCertificate certificate)
      throws NoApiKeyException, UploadFileException {
    boolean hasUpload = false;
    OSSUploadCertificate cert = certificate;
    List<Map<String, Object>> content = new ArrayList<>();

    for (Map<String, Object> item : messages.getContent()) {
      content.add(new HashMap<>(item));
    }
    for (Map<String, Object> item : content) {
      for (Map.Entry<String, Object> entry : item.entrySet()) {
        CheckAndUploadResult result = checkAndUploadMultiModalMessage(
            model, entry, apiKey, cert);
        if (result.isUpload() && !hasUpload) {
          hasUpload = true;
        }
        cert = result.getCertificate();
      }
    }
    messages.setContent(content);
    return new PreprocessResult(hasUpload, cert);
  }

  /**
   * Preprocess multimodal message inputs without certificate reuse.
   *
   * @param model Model name
   * @param messages Multimodal message
   * @param apiKey API key
   * @return true if any file was uploaded
   * @throws NoApiKeyException If API key is missing
   * @throws UploadFileException If upload fails
   */
  public static boolean preProcessMultiModalMessageInputs(
      String model, MultiModalMessage messages, String apiKey)
      throws NoApiKeyException, UploadFileException {
    PreprocessResult result = preProcessMultiModalMessageInputs(model,
        messages, apiKey, null);
    return result.hasUpload();
  }

  /**
   * Result of check and upload operation.
   */
  public static class CheckAndUploadResult {
    private boolean upload;
    private OSSUploadCertificate certificate;

    public CheckAndUploadResult(boolean upload,
        OSSUploadCertificate certificate) {
      this.upload = upload;
      this.certificate = certificate;
    }

    public boolean isUpload() {
      return upload;
    }

    public OSSUploadCertificate getCertificate() {
      return certificate;
    }
  }

  /**
   * Result of check and upload one operation.
   */
  public static class CheckAndUploadOneResult {
    private String fileUrl;
    private OSSUploadCertificate certificate;

    public CheckAndUploadOneResult(String fileUrl,
        OSSUploadCertificate certificate) {
      this.fileUrl = fileUrl;
      this.certificate = certificate;
    }

    public String getFileUrl() {
      return fileUrl;
    }

    public OSSUploadCertificate getCertificate() {
      return certificate;
    }
  }

  /**
   * Result of preprocess operation.
   */
  public static class PreprocessResult {
    private boolean hasUpload;
    private OSSUploadCertificate certificate;

    public PreprocessResult(boolean hasUpload,
        OSSUploadCertificate certificate) {
      this.hasUpload = hasUpload;
      this.certificate = certificate;
    }

    public boolean hasUpload() {
      return hasUpload;
    }

    public OSSUploadCertificate getCertificate() {
      return certificate;
    }
  }
}
