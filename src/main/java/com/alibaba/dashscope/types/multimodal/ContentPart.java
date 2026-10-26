// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.multimodal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One multimodal content part (design §4): the wire shape is a single-key object — {@code {"text":
 * "..."}}, {@code {"image": "<url>"}}, {@code {"audio": "<url>"}}, {@code {"video": ["f1","f2"]}} —
 * with arbitrary keys allowed (forward compatibility). Values may be String or List<String>.
 * Instances are immutable.
 */
public final class ContentPart {
  private final LinkedHashMap<String, Object> entries;

  private ContentPart(LinkedHashMap<String, Object> entries) {
    this.entries = entries;
  }

  public static ContentPart of(String key, Object value) {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put(key, value);
    return new ContentPart(map);
  }

  public static ContentPart text(String text) {
    return of("text", text);
  }

  public static ContentPart image(String url) {
    return of("image", url);
  }

  public static ContentPart audio(String url) {
    return of("audio", url);
  }

  /** Video as a frame list (wire value is List<String>) or a single video URL. */
  public static ContentPart video(List<String> frames) {
    return of("video", new ArrayList<>(frames));
  }

  public static ContentPart video(String url) {
    return of("video", url);
  }

  @JsonCreator
  public static ContentPart fromMap(Map<String, Object> map) {
    return new ContentPart(new LinkedHashMap<>(map));
  }

  @JsonValue
  public Map<String, Object> asMap() {
    return Collections.unmodifiableMap(entries);
  }

  public boolean has(String key) {
    return entries.containsKey(key);
  }

  public Object get(String key) {
    return entries.get(key);
  }

  /** The text of a text part, else null. */
  public String getText() {
    Object value = entries.get("text");
    return value instanceof String ? (String) value : null;
  }

  /** Copy with one entry replaced (local-file preprocessing swaps file:// for oss://). */
  public ContentPart withValue(String key, Object value) {
    LinkedHashMap<String, Object> copy = new LinkedHashMap<>(entries);
    copy.put(key, value);
    return new ContentPart(copy);
  }
}
