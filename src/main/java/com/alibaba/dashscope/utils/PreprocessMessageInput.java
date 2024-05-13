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

  public static boolean checkAndUpload(
      String model, MultiModalMessageItemBase message, String apiKey)
      throws NoApiKeyException, UploadFileException {
    boolean isUpload = false;
    if (!message.getModal().equals("text")
        && message.getContent().startsWith(ApiKeywords.FILE_PATH_SCHEMA)) {
      try {
        URI fileURI = new URI(message.getContent());
        File f = new File(fileURI);
        if (f.exists()) {
          String fileUrl = OSSUtils.upload(model, f.getAbsolutePath(), apiKey);
          if (fileUrl == null) {
            throw new UploadFileException(
                String.format("Uploading file: %s failed", message.getContent()));
          }
          message.setContent(fileUrl);
          isUpload = true;
        } else {
          throw new UploadFileException(
              String.format("Local file: %s not exists.", message.getContent()));
        }
      } catch (URISyntaxException e) {
        throw new UploadFileException(e.getMessage());
      }
    } else if (!message.getModal().equals("text") && !message.getContent().startsWith("http")) {
      if (isValidPath(message.getContent())) {
        File f = new File(message.getContent());
        if (f.exists()) {
          String fileUrl = OSSUtils.upload(model, f.getAbsolutePath(), apiKey);
          if (fileUrl == null) {
            throw new UploadFileException(
                String.format("Uploading file: %s failed", message.getContent()));
          }
          message.setContent(fileUrl);
          isUpload = true;
        }
      }
    }
    return isUpload;
  }

  public static <T extends MultiModalMessageItemBase> boolean preProcessMessageInputs(
      String model, List<T> messages, String apiKey) throws NoApiKeyException, UploadFileException {
    boolean hasUpload = false;
    for (MultiModalMessageItemBase elem : messages) {
      boolean isUpload = checkAndUpload(model, elem, apiKey);
      if (isUpload && !hasUpload) {
        hasUpload = true;
      }
    }
    return hasUpload;
  }

  public static boolean checkAndUploadMultiModalMessage(
      String model, Map.Entry<String, Object> entry, String apiKey)
      throws NoApiKeyException, UploadFileException {
    boolean isUpload = false;
    if (entry.getValue() instanceof String) {
      String v = (String) entry.getValue();
      if (v.startsWith(ApiKeywords.FILE_PATH_SCHEMA)) {
        try {
          URI fileURI = new URI(v);
          File f = new File(fileURI);
          if (f.exists()) {
            String fileUrl = OSSUtils.upload(model, f.getAbsolutePath(), apiKey);
            if (fileUrl == null) {
              throw new UploadFileException(String.format("Uploading file: %s failed", v));
            }
            entry.setValue(fileUrl);
            isUpload = true;
          } else {
            throw new UploadFileException(String.format("Local file: %s not exists.", v));
          }
        } catch (URISyntaxException e) {
          throw new UploadFileException(e.getMessage());
        }
      } else if (!entry.getKey().equals("text") && !v.startsWith("http")) {
        if (isValidPath(v)) {
          File f = new File(v);
          if (f.exists()) {
            String fileUrl = OSSUtils.upload(model, f.getAbsolutePath(), apiKey);
            if (fileUrl == null) {
              throw new UploadFileException(String.format("Uploading file: %s failed", v));
            }
            entry.setValue(fileUrl);
            isUpload = true;
          }
        }
      }
    }
    return isUpload;
  }

  public static boolean preProcessMultiModalMessageInputs(
      String model, MultiModalMessage messages, String apiKey)
      throws NoApiKeyException, UploadFileException {
    boolean hasUpload = false;
    for (Map<String, Object> item : messages.getContent()) {
      for (Map.Entry<String, Object> entry : item.entrySet()) {
        boolean isUpload = checkAndUploadMultiModalMessage(model, entry, apiKey);
        if (isUpload && !hasUpload) {
          hasUpload = true;
        }
      }
    }
    return hasUpload;
  }
}
