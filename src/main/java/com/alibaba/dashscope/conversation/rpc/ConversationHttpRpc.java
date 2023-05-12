// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.conversation.rpc;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.common.ErrorType;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.conversation.ConversationMessageStatus;
import com.alibaba.dashscope.conversation.ConversationParam;
import com.alibaba.dashscope.conversation.ConversationResult;
import com.alibaba.dashscope.conversation.EventType;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.HttpFactory;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;

@Slf4j
public final class ConversationHttpRpc implements ConversationRpc {

  @Override
  public void call(ConversationParam param, ResultCallback<ConversationResult> callback) {
    String url = Constants.baseHttpApiUrl;
    if (param.getApiKey() == null && Constants.apiKey == null) {
      callback.onError(
          new ApiException(
              ConversationMessageStatus.builder()
                  .statusCode(HttpStatus.SC_UNAUTHORIZED)
                  .code(ErrorType.API_KEY_ERROR.getValue())
                  .message(ErrorType.API_KEY_ERROR.getValue())
                  .build()));
      return;
    }
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    url = url + param.url();
    HttpPost request = new HttpPost(url);
    request.addHeader(
        "Authorization",
        "Bearer " + (param.getApiKey() == null ? Constants.apiKey : param.getApiKey()));
    request.addHeader("Content-Type", "application/json");
    if (param.isStream()) {
      request.addHeader("Accept", "text/event-stream");
      request.addHeader("X-Accel-Buffering", "no");
      request.addHeader("X-DashScope-SSE", "enable");
    } else {
      request.addHeader("Accept", "application/json");
    }

    if (param.isSecurityCheck()) {
      request.addHeader("X-DashScope-DataInspection", "enable");
    }

    try {
      StringEntity entity =
          new StringEntity(param.buildMessageBody(Protocol.HTTP.getValue()), "UTF-8");
      request.setEntity(entity);
    } catch (ApiException e) {
      callback.onError(e);
      return;
    }
    send(request, callback, param.isStream(), param.resultType(), param.getMsgId());
  }

  private void send(
      HttpRequestBase queryMethod,
      ResultCallback<ConversationResult> callback,
      boolean stream,
      Class<? extends ConversationResult> resultType,
      String msgId) {
    try {
      HttpFactory.getInstance()
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
                        ConversationResult result = null;
                        try (InputStream inputStream = entity.getContent();
                            BufferedReader reader =
                                new BufferedReader(
                                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                          if (stream) {
                            boolean lastIsError = false;
                            while ((line = reader.readLine()) != null) {
                              if (line.startsWith("event:error")
                                  || line.startsWith("event: error")) {
                                lastIsError = true;
                              } else if (line.startsWith("data:")) {
                                line = line.substring(5).trim();
                                if (lastIsError) {
                                  ConversationMessageStatus status =
                                      parseMessageStatus(
                                          line, HttpStatus.SC_INTERNAL_SERVER_ERROR, response);
                                  callback.onError(new ApiException(status));
                                } else {
                                  result = resultType.newInstance();
                                  result.loadFromMessage(Protocol.HTTP.getValue(), line);
                                  result.setEventType(EventType.RESULT_GENERATED.getValue());
                                  callback.onEvent(EventType.RESULT_GENERATED.getValue(), result);
                                }
                              }
                            }
                            if (!lastIsError) {
                              if (result != null) {
                                // TODO a low efficient way.
                                String tmp = JsonUtils.toJson(result);
                                result = JsonUtils.fromJson(tmp, resultType);
                                result.setEventType(EventType.TASK_FINISHED.getValue());
                              }
                              callback.onEvent(EventType.TASK_FINISHED.getValue(), result);
                            }
                          } else {
                            StringBuilder content = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                              content.append(line);
                              content.append(System.lineSeparator());
                            }

                            result = resultType.newInstance();
                            result.loadFromMessage(Protocol.HTTP.getValue(), content.toString());
                            result.setEventType(EventType.TASK_FINISHED.getValue());
                            callback.onEvent(EventType.TASK_FINISHED.getValue(), result);
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
                      ConversationMessageStatus status =
                          parseMessageStatus(
                              content != null ? content.toString() : null, statusCode, response);
                      callback.onError(new ApiException(status));
                    }
                  } catch (Exception e) {
                    callback.onError(new ApiException(e));
                  }
                }

                @Override
                public void failed(final Exception ex) {
                  callback.onError(new ApiException(ex));
                }

                @Override
                public void cancelled() {
                  ConversationMessageStatus status =
                      ConversationMessageStatus.builder()
                          .code(ErrorType.REQUEST_CANCELLED.getValue())
                          .msgId(msgId)
                          .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                          .build();
                  callback.onError(new ApiException(status));
                }
              });
    } catch (Exception e) {
      callback.onError(new ApiException(e));
    }
  }

  private ConversationMessageStatus parseMessageStatus(
      String payload, int statusCode, org.apache.http.HttpResponse response) {
    if (payload == null) {
      return ConversationMessageStatus.builder()
          .statusCode(statusCode)
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(ErrorType.RESPONSE_ERROR.getValue())
          .build();
    }
    boolean isJson = false;
    Header header = response.getFirstHeader("Content-Type");
    if (header.getValue() != null
        && (header.getValue().toLowerCase().contains("application/json")
            || header.getValue().toLowerCase().contains("text/event-stream"))) {
      isJson = true;
    }
    if (isJson) {
      JsonObject json = JsonUtils.parse(payload);
      return ConversationMessageStatus.builder()
          .code(json.get(CODE) == null ? null : json.get(CODE).getAsString())
          .statusCode(statusCode)
          .message(json.get(MESSAGE) == null ? null : json.get(MESSAGE).getAsString())
          .usage(json.getAsJsonObject(USAGE))
          .msgId(json.get(REQUEST_ID) == null ? null : json.get(REQUEST_ID).getAsString())
          .build();
    } else {
      return ConversationMessageStatus.builder()
          .statusCode(statusCode)
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(payload)
          .build();
    }
  }
}
