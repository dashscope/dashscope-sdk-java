// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolCallFunction.CallFunction;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageAdapter extends TypeAdapter<Message> {
  @Override
  public void write(JsonWriter out, Message value) throws IOException {
    out.beginObject();
    out.name(ApiKeywords.ROLE);
    out.value(value.getRole());
    if (value.getContent() != null) {
      out.name(ApiKeywords.CONTENT);
      out.value(value.getContent());
    }
    // array of content
    if (value.getContents() != null) {
      out.name(ApiKeywords.CONTENT).beginArray();
      for (MessageContentBase content : value.getContents()) {
        if (content.getType().equals(ApiKeywords.CONTENT_TYPE_TEXT)) {
          // text content
          MessageContentText textContent = (MessageContentText) content;
          out.beginObject();
          out.name("type");
          out.value(ApiKeywords.CONTENT_TYPE_TEXT);
          out.name(ApiKeywords.CONTENT_TYPE_TEXT);
          out.value(textContent.getText());

          // Write cache_control field if present
          if (textContent.getCacheControl() != null) {
            out.name(ApiKeywords.CONTENT_TYPE_CACHE_CONTROL);
            out.beginObject();
            out.name("type");
            out.value(textContent.getCacheControl().getType());
            out.name("ttl");
            out.value(textContent.getCacheControl().getTtl());
            out.endObject();
          }

          out.endObject();
        } else if (content.getType().equals(ApiKeywords.CONTENT_TYPE_IMAGE_URL)) {
          out.beginObject();
          out.name("type");
          out.value(ApiKeywords.CONTENT_TYPE_IMAGE_URL);
          out.name(ApiKeywords.CONTENT_TYPE_IMAGE_URL);
          out.beginObject();
          out.name("url");
          ImageURL imageURL = ((MessageContentImageURL) content).getImageURL();
          out.value(imageURL.getUrl());
          if (imageURL.getDetail() != null) {
            out.name("detail");
            out.value(imageURL.getDetail());
          }
          out.endObject();
          out.endObject(); // image url
        }
        // not support type, TODO extends types.
      }
      out.endArray();
    }
    if (value.getReasoningContent() != null) {
      out.name(ApiKeywords.REASONING_CONTENT);
      out.value(value.getReasoningContent());
    }
    if (value.getToolCalls() != null) {
      out.name(ApiKeywords.TOOL_CALLS).beginArray();
      for (ToolCallBase toolCall : value.getToolCalls()) {
        out.beginObject();
        String type = toolCall.getType();
        out.name("type");
        out.value(type);
        out.name("id");
        out.value(toolCall.getId());
        out.name("index");
        out.value(toolCall.getIndex());
        out.name("function");
        if (type.equals("function")) {
          writeCallFunction(out, (ToolCallFunction) toolCall);
        }
        out.endObject();
      }
      out.endArray();
    }
    if (value.getToolCallId() != null) {
      out.name(ApiKeywords.TOOL_CALL_ID);
      out.value(value.getToolCallId());
    }
    if (value.getName() != null) {
      out.name(ApiKeywords.NAME);
      out.value(value.getName());
    }
    if (value.getPartial() != null) {
      out.name(ApiKeywords.PARTIAL);
      out.value(value.getPartial());
    }
    out.endObject();
  }

  private void writeCallFunction(JsonWriter out, ToolCallFunction toolCall) throws IOException {
    out.beginObject();
    CallFunction callFunction = toolCall.getFunction();
    out.name("name");
    out.value(callFunction.getName());
    out.name("arguments");
    out.value(callFunction.getArguments());
    out.endObject();
  }

  // Parse array of content objects
  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<MessageContentBase> parseContentList(List<LinkedTreeMap> contentList) {
    List<MessageContentBase> contents = new ArrayList<>();
    for (LinkedTreeMap<String, Object> contentItem : contentList) {
      String type = (String) contentItem.get("type");
      if (ApiKeywords.CONTENT_TYPE_TEXT.equals(type)) {
        contents.add(parseTextContent(contentItem));
      } else if (ApiKeywords.CONTENT_TYPE_IMAGE_URL.equals(type)) {
        contents.add(parseImageContent(contentItem));
      }
    }
    return contents;
  }

  // Parse text content with optional cache_control
  @SuppressWarnings({"unchecked", "rawtypes"})
  private MessageContentText parseTextContent(LinkedTreeMap<String, Object> contentItem) {
    MessageContentText.MessageContentTextBuilder textBuilder =
        MessageContentText.builder()
            .type((String) contentItem.get("type"))
            .text((String) contentItem.get(ApiKeywords.CONTENT_TYPE_TEXT));

    // Parse cache_control if present
    if (contentItem.containsKey(ApiKeywords.CONTENT_TYPE_CACHE_CONTROL)) {
      LinkedTreeMap<String, Object> cacheControlMap =
          (LinkedTreeMap<String, Object>) contentItem.get(
              ApiKeywords.CONTENT_TYPE_CACHE_CONTROL);
      MessageContentText.CacheControl.CacheControlBuilder cacheBuilder =
          MessageContentText.CacheControl.builder()
              .type((String) cacheControlMap.get("type"));

      // Handle ttl field - convert to String regardless of input type
      if (cacheControlMap.containsKey("ttl")) {
        Object ttlObj = cacheControlMap.get("ttl");
        cacheBuilder.ttl(ttlObj instanceof Number
            ? String.valueOf(((Number) ttlObj).intValue())
            : String.valueOf(ttlObj));
      }
      textBuilder.cacheControl(cacheBuilder.build());
    }
    return textBuilder.build();
  }

  // Parse image_url content
  @SuppressWarnings({"unchecked", "rawtypes"})
  private MessageContentImageURL parseImageContent(
      LinkedTreeMap<String, Object> contentItem) {
    LinkedTreeMap<String, Object> imageUrlMap =
        (LinkedTreeMap<String, Object>) contentItem.get(
            ApiKeywords.CONTENT_TYPE_IMAGE_URL);
    ImageURL.ImageURLBuilder imageBuilder =
        ImageURL.builder().url((String) imageUrlMap.get("url"));
    if (imageUrlMap.containsKey("detail")) {
      imageBuilder.detail((String) imageUrlMap.get("detail"));
    }
    return MessageContentImageURL.builder()
        .type((String) contentItem.get("type"))
        .imageURL(imageBuilder.build())
        .build();
  }

  @SuppressWarnings("unchecked")
  private ToolCallFunction convertToCallFunction(LinkedTreeMap<String, Object> toolCall) {
    ToolCallFunction functionCall = new ToolCallFunction();
    if (toolCall.containsKey("function")) {
      ToolCallFunction.CallFunction callFunction = functionCall.new CallFunction();
      LinkedTreeMap<String, Object> fc = (LinkedTreeMap<String, Object>) toolCall.get("function");
      if (fc.containsKey("name")) {
        callFunction.setName(fc.get("name").toString());
      }
      if (fc.containsKey("arguments")) {
        callFunction.setArguments(fc.get("arguments").toString());
      }
      functionCall.setFunction(callFunction);
    }
    functionCall.setType(toolCall.get("type").toString());
    if (toolCall.containsKey("id")) {
      functionCall.setId(toolCall.get("id").toString());
    }
    // Fix: Set index field if it exists in the JSON
    if (toolCall.containsKey("index")) {
      Object indexObj = toolCall.get("index");
      if (indexObj instanceof Number) {
        functionCall.setIndex(((Number) indexObj).intValue());
      }
    }
    return functionCall;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Message read(JsonReader in) throws IOException {
    Map<String, Object> objectMap = JsonUtils.gson.fromJson(in, Map.class);
    Message msg = new Message();

    if (objectMap.containsKey(ApiKeywords.ROLE)) {
      msg.setRole((String) objectMap.get(ApiKeywords.ROLE));
      objectMap.remove(ApiKeywords.ROLE);
    }

    if (objectMap.containsKey(ApiKeywords.CONTENT)) {
      Object contentObj = objectMap.get(ApiKeywords.CONTENT);
      // Handle both string and array content types
      if (contentObj instanceof String) {
        msg.setContent((String) contentObj);
      } else if (contentObj instanceof List) {
        msg.setContents(parseContentList((List<LinkedTreeMap>) contentObj));
      }
      objectMap.remove(ApiKeywords.CONTENT);
    }

    if (objectMap.containsKey(ApiKeywords.REASONING_CONTENT)) {
      msg.setReasoningContent((String) objectMap.get(ApiKeywords.REASONING_CONTENT));
      objectMap.remove(ApiKeywords.REASONING_CONTENT);
    }

    if (objectMap.containsKey(ApiKeywords.TOOL_CALLS)) {
      msg.toolCalls = new ArrayList<ToolCallBase>();
      List<LinkedTreeMap> toolCalls = (List<LinkedTreeMap>) objectMap.get(ApiKeywords.TOOL_CALLS);
      if (toolCalls == null) {
        toolCalls = new ArrayList<>();
      }
      for (LinkedTreeMap<String, Object> toolCall : toolCalls) {
        // convert to toolCall
        String type = toolCall.get("type").toString();
        if (type.equals("function")) {
          msg.toolCalls.add(convertToCallFunction(toolCall));
        }
      }
      objectMap.remove(ApiKeywords.TOOL_CALLS);
    }
    return msg;
  }
}
