// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.omni;

/** @author lengjiayi */
public class OmniRealtimeConstants {
  public static final String MODALITIES = "modalities";
  public static final String VOICE = "voice";
  public static final String INPUT_AUDIO_FORMAT = "input_audio_format";
  public static final String OUTPUT_AUDIO_FORMAT = "output_audio_format";
  public static final String INPUT_AUDIO_TRANSCRIPTION = "input_audio_transcription";
  public static final String INPUT_AUDIO_TRANSCRIPTION_MODEL = "model";
  public static final String TURN_DETECTION = "turn_detection";
  public static final String TURN_DETECTION_TYPE = "type";
  public static final String TURN_DETECTION_THRESHOLD = "threshold";
  public static final String PREFIX_PADDING_MS = "prefix_padding_ms";
  public static final String SILENCE_DURATION_MS = "silence_duration_ms";

  public static final String PROTOCOL_EVENT_ID = "event_id";
  public static final String PROTOCOL_TYPE = "type";
  public static final String PROTOCOL_SESSION = "session";
  public static final String PROTOCOL_AUDIO = "audio";
  public static final String PROTOCOL_VIDEO = "image";
  public static final String PROTOCOL_EVENT_TYPE_UPDATE_SESSION = "session.update";
  public static final String PROTOCOL_EVENT_TYPE_APPEND_AUDIO = "input_audio_buffer.append";
  public static final String PROTOCOL_EVENT_TYPE_APPEND_VIDEO = "input_image_buffer.append";
  public static final String PROTOCOL_EVENT_TYPE_COMMIT = "input_audio_buffer.commit";
  public static final String PROTOCOL_EVENT_TYPE_CLEAR_AUDIO = "input_audio_buffer.clear";
  public static final String PROTOCOL_EVENT_TYPE_CREATE_RESPONSE = "response.create";
  public static final String PROTOCOL_EVENT_TYPE_CANCEL_RESPONSE = "response.cancel";
  public static final String PROTOCOL_RESPONSE_TYPE_SESSION_CREATED = "session.created";
  public static final String PROTOCOL_RESPONSE_TYPE_RESPONSE_CREATED = "response.created";
  public static final String PROTOCOL_RESPONSE_TYPE_AUDIO_TRANSCRIPT_DELTA =
      "response.audio_transcript.delta";
  public static final String PROTOCOL_RESPONSE_TYPE_AUDIO_DELTA = "response.audio.delta";
  public static final String PROTOCOL_RESPONSE_TYPE_RESPONSE_DONE = "response.done";
}
