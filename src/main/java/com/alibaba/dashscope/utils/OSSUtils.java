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
  public static String upload(String model, String filePath, String apiKey)
      throws NoApiKeyException {
    OkHttpClient client = OkHttpClientFactory.getOkHttpClient();
    DashScopeResult uploadInfo = get_upload_certificate(model, apiKey);
    JsonObject outputData = ((JsonObject) uploadInfo.getOutput()).getAsJsonObject("data");
    Map<String, String> headers = new HashMap<>();
    headers.put("user-agent", DashScopeHeaders.userAgent());
    headers.put("Accept", "application/json");
    File uploadFile = new File(filePath);
    String host = outputData.get("upload_host").getAsString();
    String ossAccessKeyId = outputData.get("oss_access_key_id").getAsString();
    String signature = outputData.get("signature").getAsString();
    String policy = outputData.get("policy").getAsString();
    String key = outputData.get("upload_dir").getAsString() + "/" + uploadFile.getName();
    String xOssObjectAcl = outputData.get("x_oss_object_acl").getAsString();
    String xOssForbidOverwrite = outputData.get("x_oss_forbid_overwrite").getAsString();

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

    Request request = new Request.Builder().url(host).post(requestBody).build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        Status status = parseFailed(response);
        throw new ApiException(status);
      }
      return String.format("oss://%s", key);
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
