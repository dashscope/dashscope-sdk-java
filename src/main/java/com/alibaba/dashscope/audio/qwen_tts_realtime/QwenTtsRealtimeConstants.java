// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.qwen_tts_realtime;

/** @author lengjiayi */
public class QwenTtsRealtimeConstants {
  public static final String MODALITIES = "modalities";
  public static final String VOICE = "voice";
  public static final String MODE = "mode";
  public static final String RESPONSE_FORMAT = "response_format";
  public static final String SAMPLE_RATE = "sample_rate";
  public static final String LANGUAGE_TYPE = "language_type";
  public static final String PROTOCOL_EVENT_ID = "event_id";
  public static final String PROTOCOL_TYPE = "type";
  public static final String PROTOCOL_SESSION = "session";
  public static final String PROTOCOL_TEXT = "text";
  public static final String PROTOCOL_EVENT_TYPE_UPDATE_SESSION = "session.update";
  public static final String PROTOCOL_EVENT_TYPE_APPEND_TEXT = "input_text_buffer.append";
  public static final String PROTOCOL_EVENT_TYPE_COMMIT = "input_text_buffer.commit";
  public static final String PROTOCOL_EVENT_TYPE_CLEAR_TEXT = "input_text_buffer.clear";
  public static final String PROTOCOL_EVENT_TYPE_CANCEL_RESPONSE = "response.cancel";
  public static final String PROTOCOL_EVENT_SESSION_FINISH = "session.finish";
  public static final String PROTOCOL_RESPONSE_TYPE_SESSION_CREATED = "session.created";
  public static final String PROTOCOL_RESPONSE_TYPE_RESPONSE_CREATED = "response.created";
  public static final String PROTOCOL_RESPONSE_TYPE_AUDIO_DELTA = "response.audio.delta";
  public static final String PROTOCOL_RESPONSE_TYPE_RESPONSE_DONE = "response.done";
}
