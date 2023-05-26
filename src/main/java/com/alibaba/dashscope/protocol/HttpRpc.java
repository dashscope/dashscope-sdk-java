// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.ErrorType;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.pool.HttpPool;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;

/** The Http rpc class, biz logic free. */
@Slf4j
public final class HttpRpc {

  private final String url;

  private final ResultCallback<Response> responseCallback;

  /**
   * Construct the class.
   *
   * @param url The connecting url.
   * @param responseCallback The response callback to receive the message from the server.
   */
  public HttpRpc(String url, ResultCallback<Response> responseCallback) {
    this.url = url;
    this.responseCallback = responseCallback;
  }

  /**
   * Do post.
   *
   * @param headers The headers.
   * @param input The input request.
   */
  public void post(Map<String, String> headers, Request input) {
    HttpPost request = new HttpPost(url);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      request.addHeader(header.getKey(), header.getValue());
    }

    try {
      StringEntity entity = new StringEntity(input.getMessage(), "UTF-8");
      request.setEntity(entity);
    } catch (ApiException e) {
      responseCallback.onError(e);
      return;
    }
    send(request, false);
  }

  /**
   * Do get.
   *
   * @param headers The headers.
   */
  public void get(Map<String, String> headers) {
    HttpGet request = new HttpGet(url);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      request.addHeader(header.getKey(), header.getValue());
    }
    send(request, false);
  }

  /**
   * Do sse.
   *
   * @param headers The headers.
   * @param input The input request.
   */
  public void sse(Map<String, String> headers, Request input) {
    HttpPost request = new HttpPost(url);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      request.addHeader(header.getKey(), header.getValue());
    }

    try {
      StringEntity entity = new StringEntity(input.getMessage(), "UTF-8");
      request.setEntity(entity);
    } catch (ApiException e) {
      responseCallback.onError(e);
      return;
    }
    send(request, true);
  }

  private void send(HttpRequestBase queryMethod, boolean sse) {
    try {
      HttpPool.getInstance()
          .getHttpClient()
          .execute(
              queryMethod,
              new FutureCallback<org.apache.http.HttpResponse>() {

                @Override
                public void completed(final org.apache.http.HttpResponse response) {
                  try {
                    int statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    if (statusCode == HttpStatus.SC_OK) {
                      if (entity != null) {
                        String line;
                        try (InputStream inputStream = entity.getContent();
                            BufferedReader reader =
                                new BufferedReader(
                                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                          if (sse) {
                            boolean lastIsError = false;
                            while ((line = reader.readLine()) != null) {
                              if (line.startsWith("event:error")
                                  || line.startsWith("event: error")) {
                                lastIsError = true;
                              } else if (line.startsWith("data:")) {
                                line = line.substring(5).trim();
                                if (lastIsError) {
                                  Status status =
                                      parseMessageStatus(
                                          line, HttpStatus.SC_INTERNAL_SERVER_ERROR, response);
                                  responseCallback.onError(new ApiException(status));
                                } else {
                                  responseCallback.onEvent(
                                      Response.builder()
                                          .headers(
                                              Arrays.stream(response.getAllHeaders())
                                                  .collect(
                                                      Collectors.toMap(
                                                          NameValuePair::getName,
                                                          NameValuePair::getValue)))
                                          .message(line)
                                          .build());
                                }
                              }
                            }
                            if (!lastIsError) {
                              responseCallback.onComplete();
                            }
                          } else {
                            StringBuilder content = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                              content.append(line);
                              content.append(System.lineSeparator());
                            }
                            responseCallback.onEvent(
                                Response.builder()
                                    .headers(
                                        Arrays.stream(response.getAllHeaders())
                                            .collect(
                                                Collectors.toMap(
                                                    NameValuePair::getName,
                                                    NameValuePair::getValue)))
                                    .message(content.toString())
                                    .build());
                            responseCallback.onComplete();
                          }
                        }
                      }
                    } else {
                      StringBuilder content = null;
                      if (entity != null) {
                        String line;
                        try (InputStream inputStream = entity.getContent();
                            BufferedReader reader =
                                new BufferedReader(
                                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                          content = new StringBuilder();
                          while ((line = reader.readLine()) != null) {
                            content.append(line);
                            content.append(System.lineSeparator());
                          }
                        } catch (Exception e) {
                          log.error("Error occurs when reading error information from server.", e);
                        }
                      }
                      Status status =
                          parseMessageStatus(
                              content != null ? content.toString() : null, statusCode, response);
                      responseCallback.onError(new ApiException(status));
                    }
                  } catch (Exception e) {
                    responseCallback.onError(new ApiException(e));
                  }
                }

                @Override
                public void failed(final Exception ex) {
                  responseCallback.onError(new ApiException(ex));
                }

                @Override
                public void cancelled() {
                  Status status =
                      Status.builder()
                          .code(ErrorType.REQUEST_CANCELLED.getValue())
                          .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                          .build();
                  responseCallback.onError(new ApiException(status));
                }
              });
    } catch (Exception e) {
      responseCallback.onError(new ApiException(e));
    }
  }

  private Status parseMessageStatus(String payload, int statusCode, HttpResponse response) {
    if (payload == null) {
      return Status.builder()
          .statusCode(statusCode)
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(ErrorType.RESPONSE_ERROR.getValue())
          .build();
    }
    boolean isJson = false;
    Header header = response.getFirstHeader("Content-Type");
    if (header != null
        && header.getValue() != null
        && (header.getValue().toLowerCase().contains("application/json")
            || header.getValue().toLowerCase().contains("text/event-stream"))) {
      isJson = true;
    }
    return Status.builder()
        .statusCode(statusCode)
        .code(ErrorType.RESPONSE_ERROR.getValue())
        .message(payload)
        .isJson(isJson)
        .build();
  }
}
