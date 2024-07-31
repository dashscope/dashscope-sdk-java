// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.EncryptionUtils;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DashScopeResult extends Result {
  private Object output;
  private String event;

  public Boolean isBinaryOutput() {
    return output instanceof ByteBuffer;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <T extends Result> T fromResponse(Protocol protocol, NetworkResponse response)
      throws ApiException {
    if (protocol == Protocol.WEBSOCKET) {
      if (response.getBinary() == null) {
        JsonObject jsonObject = JsonUtils.parse(response.getMessage());
        if (jsonObject.has(ApiKeywords.HEADER)) {
          JsonObject headers = jsonObject.get(ApiKeywords.HEADER).getAsJsonObject();
          if (headers.has(ApiKeywords.TASKID)) {
            this.setRequestId(headers.get(ApiKeywords.TASKID).getAsString());
          }
        }
        if (jsonObject.has(ApiKeywords.PAYLOAD)) {
          JsonObject payload = jsonObject.getAsJsonObject(ApiKeywords.PAYLOAD);
          if (payload.has(ApiKeywords.OUTPUT)) {
            this.output =
                payload.get(ApiKeywords.OUTPUT).isJsonNull()
                    ? null
                    : payload.get(ApiKeywords.OUTPUT);
          }
          if (payload.has(ApiKeywords.USAGE)) {
            this.setUsage(
                payload.get(ApiKeywords.USAGE).isJsonNull()
                    ? null
                    : payload.get(ApiKeywords.USAGE));
          }
        }
      } else {
        this.output = response.getBinary();
      }
    } else {
      JsonObject jsonObject = JsonUtils.parse(response.getMessage());
      if (jsonObject.has(ApiKeywords.OUTPUT)) {
        this.output =
            jsonObject.get(ApiKeywords.OUTPUT).isJsonNull()
                ? null
                : jsonObject.get(ApiKeywords.OUTPUT).getAsJsonObject();
      }
      if (jsonObject.has(ApiKeywords.USAGE)) {
        this.setUsage(
            jsonObject.get(ApiKeywords.USAGE).isJsonNull()
                ? null
                : jsonObject.get(ApiKeywords.USAGE).getAsJsonObject());
      }
      if (jsonObject.has(ApiKeywords.REQUEST_ID)) {
        this.setRequestId(jsonObject.get(ApiKeywords.REQUEST_ID).getAsString());
      }
      if (jsonObject.has(ApiKeywords.DATA)) {
        if (jsonObject.has(ApiKeywords.REQUEST_ID)) {
          jsonObject.remove(ApiKeywords.REQUEST_ID);
        }
        this.output = jsonObject;
      }
    }
    return (T) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult) throws ApiException {
    if (!isFlattenResult) {
      return fromResponse(protocol, response);
    } else {
      // flatten not support websocket.
      if (protocol == Protocol.WEBSOCKET) {
        if (response.getBinary() == null) {
          JsonObject jsonObject = JsonUtils.parse(response.getMessage());
          this.output = jsonObject;
          // convert to the result
        } else {
          this.output = response.getBinary();
        }
      } else { // HTTP
        JsonObject jsonObject = JsonUtils.parse(response.getMessage());
        this.output = jsonObject;
        this.event = response.getEvent();
      }
      return (T) this;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult, HalfDuplexRequest req)
      throws ApiException {
    // check it's encrypted output
    if ((response.getHeaders().containsKey("X-DashScope-OutputEncrypted".toLowerCase())
            || req.isEncryptRequest())
        && protocol == Protocol.HTTP) {
      JsonObject jsonObject = JsonUtils.parse(response.getMessage());
      String encryptedOutput =
          jsonObject.get(ApiKeywords.OUTPUT).isJsonNull()
              ? null
              : jsonObject.get(ApiKeywords.OUTPUT).getAsString();
      if (encryptedOutput != null) {
        String plainOutput =
            EncryptionUtils.AESDecrypt(
                encryptedOutput,
                req.getEncryptionConfig().getAESEncryptKey(),
                req.getEncryptionConfig().getIv());
        this.output = JsonUtils.parse(plainOutput);
      } else {
        this.output = null;
      }
      if (jsonObject.has(ApiKeywords.USAGE)) {
        this.setUsage(
            jsonObject.get(ApiKeywords.USAGE).isJsonNull()
                ? null
                : jsonObject.get(ApiKeywords.USAGE).getAsJsonObject());
      }
      if (jsonObject.has(ApiKeywords.REQUEST_ID)) {
        this.setRequestId(jsonObject.get(ApiKeywords.REQUEST_ID).getAsString());
      }
      if (jsonObject.has(ApiKeywords.DATA)) {
        if (jsonObject.has(ApiKeywords.REQUEST_ID)) {
          jsonObject.remove(ApiKeywords.REQUEST_ID);
        }
      }
      return (T) this;
    }
    return fromResponse(protocol, response, isFlattenResult);
  }
}
