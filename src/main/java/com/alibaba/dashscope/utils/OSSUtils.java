package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ErrorType;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.DashScopeHeaders;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.okhttp.OkHttpClientFactory;
import com.google.gson.JsonObject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileTypeDetector;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public final class OSSUtils {
  /**
   * Upload file to OSS without certificate reuse.
   *
   * @param model Model name
   * @param filePath Local file path
   * @param apiKey API key
   * @return OSS URL
   * @throws NoApiKeyException If API key is missing
   */
  public static String upload(String model, String filePath, String apiKey)
      throws NoApiKeyException {
    UploadResult result = uploadWithCertificate(model, filePath, apiKey,
        null);
    return result.getOssUrl();
  }

  /**
   * Upload file to OSS with optional certificate reuse.
   *
   * @param model Model name
   * @param filePath Local file path
   * @param apiKey API key
   * @param certificate Optional upload certificate for reuse
   * @return UploadResult containing OSS URL and certificate
   * @throws NoApiKeyException If API key is missing
   */
  public static UploadResult uploadWithCertificate(String model,
      String filePath, String apiKey, OSSUploadCertificate certificate)
      throws NoApiKeyException {
    OkHttpClient client = OkHttpClientFactory.getOkHttpClient();
    OSSUploadCertificate cert = certificate;

    // Get certificate if not provided
    if (cert == null) {
      DashScopeResult uploadInfo = get_upload_certificate(model, apiKey);
      JsonObject outputData = ((JsonObject) uploadInfo.getOutput())
          .getAsJsonObject("data");
      cert = new OSSUploadCertificate(
          outputData.get("upload_host").getAsString(),
          outputData.get("oss_access_key_id").getAsString(),
          outputData.get("signature").getAsString(),
          outputData.get("policy").getAsString(),
          outputData.get("upload_dir").getAsString(),
          outputData.get("x_oss_object_acl").getAsString(),
          outputData.get("x_oss_forbid_overwrite").getAsString()
      );
    }

    Map<String, String> headers = new HashMap<>();
    headers.put("user-agent", DashScopeHeaders.userAgent());
    headers.put("Accept", "application/json");
    File uploadFile = new File(filePath);
    String host = cert.getUploadHost();
    String ossAccessKeyId = cert.getOssAccessKeyId();
    String signature = cert.getSignature();
    String policy = cert.getPolicy();
    String key = cert.getUploadDir() + "/" + uploadFile.getName();
    String xOssObjectAcl = cert.getXOssObjectAcl();
    String xOssForbidOverwrite = cert.getXOssForbidOverwrite();

    RequestBody requestBody =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("OSSAccessKeyId", ossAccessKeyId)
            .addFormDataPart("Signature", signature)
            .addFormDataPart("policy", policy)
            .addFormDataPart("key", key)
            .addFormDataPart("x-oss-object-acl", xOssObjectAcl)
            .addFormDataPart("x-oss-forbid-overwrite", xOssForbidOverwrite)
            .addFormDataPart("success_action_status", "200")
            .addFormDataPart("x-oss-content-type", getContentType(filePath))
            .addFormDataPart(
                "file",
                uploadFile.getName(),
                RequestBody.create(MediaType.parse(getContentType(filePath)), uploadFile))
            .build();

    Request request = new Request.Builder().url(host).post(requestBody)
        .build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        Status status = parseFailed(response);
        throw new ApiException(status);
      }
      String ossUrl = String.format("oss://%s", key);
      return new UploadResult(ossUrl, cert);
    } catch (Throwable e) {
      throw new ApiException(e);
    }
  }

  public static DashScopeResult get_upload_certificate(String model, String apiKey)
      throws NoApiKeyException {
    OkHttpClient client = OkHttpClientFactory.getOkHttpClient();
    String url = Constants.baseHttpApiUrl;
    if (url.endsWith("/")) {
      url += "uploads";
    } else {
      url += "/uploads";
    }
    HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
    httpBuilder.addQueryParameter("action", "getPolicy");
    httpBuilder.addQueryParameter("model", model);
    Request request =
        new Request.Builder()
            .url(httpBuilder.build())
            .headers(
                Headers.of(
                    DashScopeHeaders.buildHttpHeaders(
                        ApiKey.getApiKey(apiKey),
                        false,
                        Protocol.HTTP,
                        false,
                        false,
                        "",
                        new HashMap<String, String>())))
            .build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        Status status = parseFailed(response);
        throw new ApiException(status);
      }
      return new DashScopeResult()
          .fromResponse(
              Protocol.HTTP,
              NetworkResponse.builder().message(response.body().string()).build(),
              false);
    } catch (Throwable e) {
      throw new ApiException(e);
    }
  }

  private static Status parseFailed(Response response) {
    boolean isJson = false;
    String contentType = response.header("Content-Type");
    if (contentType != null && contentType.toLowerCase().contains("application/json")) {
      isJson = true;
      try {
        JsonObject jsonResponse = JsonUtils.parse(response.body().string());
        String code = "";
        String message = "";
        String requestId = "";
        if (jsonResponse.has(ApiKeywords.REQUEST_ID)) {
          requestId = jsonResponse.get(ApiKeywords.REQUEST_ID).getAsString();
        }
        if (jsonResponse.has(ApiKeywords.CODE)) {
          code = jsonResponse.get(ApiKeywords.CODE).getAsString();
        }
        if (jsonResponse.has(ApiKeywords.MESSAGE)) {
          message = jsonResponse.get(ApiKeywords.MESSAGE).getAsString();
        }
        return Status.builder()
            .statusCode(response.code())
            .code(code)
            .message(message)
            .requestId(requestId)
            .isJson(isJson)
            .build();
      } catch (Throwable e) {
        return Status.builder()
            .statusCode(response.code())
            .code(ErrorType.RESPONSE_ERROR.getValue())
            .message(response.message())
            .isJson(isJson)
            .build();
      }
    } else {
      return Status.builder()
          .statusCode(response.code())
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(response.message())
          .isJson(isJson)
          .build();
    }
  }

  public class ImageFileTypeDetector extends FileTypeDetector {
    @Override
    public String probeContentType(Path path) {
      try (InputStream in =
          new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ))) {
        return URLConnection.guessContentTypeFromStream(in);
      } catch (IOException e) {
        return URLConnection.guessContentTypeFromName(path.getFileName().toString());
      }
    }
  }

  private static String getContentType(String imagePath) {
    Path path = new File(imagePath).toPath();
    try {
      String mimeType = Files.probeContentType(path);
      if (mimeType == null) {
        if (imagePath.endsWith("mp3")) {
          mimeType = "audio/mp3";
        } else if (imagePath.endsWith("flac")) {
          mimeType = "audio/flac";
        } else if (imagePath.endsWith("wav")) {
          mimeType = "audio/wav";
        } else if (imagePath.endsWith("m4a")) {
          mimeType = "audio/mp4";
        } else if (imagePath.endsWith("png")) {
          mimeType = "image/png";
        } else if (imagePath.endsWith("jpeg") || imagePath.endsWith("jpg")) {
          mimeType = "image/jpeg";
        } else if (imagePath.endsWith("bmp")) {
          mimeType = "image/bmp";
        } else if (imagePath.endsWith("gif")) {
          mimeType = "image/gif";
        } else if (imagePath.endsWith("tiff")) {
          mimeType = "image/tiff";
        } else {
          log.error("Can not determine MIMEType, use default application/octet-stream");
          mimeType = "application/octet-stream";
        }
      }
      return mimeType;
    } catch (IOException e) {
      return "application/octet-stream";
    }
  }
}
