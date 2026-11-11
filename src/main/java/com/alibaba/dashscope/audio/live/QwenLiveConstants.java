// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

public final class QwenLiveConstants {
  private QwenLiveConstants() {}

  public static final String EVENT_SESSION_UPDATE = "session.update";
  public static final String EVENT_SESSION_INPUT_AUDIO_APPEND = "session.input_audio.append";
  public static final String EVENT_SESSION_INPUT_AUDIO_MUTE = "session.input_audio.mute";
  public static final String EVENT_SESSION_INPUT_AUDIO_UNMUTE = "session.input_audio.unmute";
  public static final String EVENT_SESSION_INSTRUCTION_APPEND = "session.instruction.append";
  public static final String EVENT_SESSION_THINKING_APPEND = "session.thinking.append";
  public static final String EVENT_SESSION_COMMENTARY_APPEND = "session.commentary.append";
  public static final String EVENT_RESPONSE_ITEM_CREATE = "response.item.create";
  public static final String EVENT_RESPONSE_CREATE = "response.create";

  public static final String EVENT_ERROR = "error";
  public static final String EVENT_SESSION_CREATED = "session.created";
  public static final String EVENT_SESSION_UPDATED = "session.updated";
  public static final String EVENT_SESSION_INPUT_AUDIO_MUTED = "session.input_audio.muted";
  public static final String EVENT_SESSION_INPUT_AUDIO_UNMUTED = "session.input_audio.unmuted";
  public static final String EVENT_SESSION_OUTPUT_AUDIO_DELTA = "session.output_audio.delta";
  public static final String EVENT_SESSION_INPUT_TRANSCRIPT_DELTA =
      "session.input_transcript.delta";
  public static final String EVENT_SESSION_OUTPUT_TRANSCRIPT_DELTA =
      "session.output_transcript.delta";
  public static final String EVENT_RESPONSE_EVENT = "response.event";
  public static final String EVENT_SESSION_USAGE_UPDATED = "session.usage.updated";
  public static final String EVENT_SESSION_CLOSED = "session.closed";
  public static final String EVENT_SESSION_DELEGATION_CREATED = "session.delegation.created";
  public static final String EVENT_SESSION_INSTRUCTIONS_APPENDED = "session.instructions.appended";

  public static final String EVENT_SESSION_THINKING_APPENDED = "session.thinking.appended";
  public static final String EVENT_SESSION_COMMENTARY_APPENDED = "session.commentary.appended";

  public static final String FIELD_TYPE = "type";
  public static final String FIELD_EVENT_ID = "event_id";
  public static final String FIELD_CLIENT_EVENT_ID = "client_event_id";
  public static final String FIELD_SESSION = "session";
  public static final String FIELD_MODEL = "model";
  public static final String FIELD_ID = "id";
  public static final String FIELD_VOICE = "voice";
  public static final String FIELD_INSTRUCTIONS = "instructions";
  public static final String FIELD_MODALITIES = "modalities";
  public static final String FIELD_TOOLS = "tools";
  public static final String FIELD_INPUT_AUDIO_FORMAT = "input_audio_format";
  public static final String FIELD_OUTPUT_AUDIO_FORMAT = "output_audio_format";
  public static final String FIELD_TRANSPORT = "transport";
  public static final String FIELD_DELEGATION = "delegation";
  public static final String FIELD_CONTEXT = "context";
  public static final String FIELD_COMPACTION = "compaction";
  public static final String FIELD_ENABLED = "enabled";
  public static final String FIELD_MODE = "mode";
  public static final String FIELD_RESPONSES = "responses";
  public static final String FIELD_AUDIO = "audio";
  public static final String FIELD_CONTENT = "content";
  public static final String FIELD_ITEM = "item";
  public static final String FIELD_DELEGATION_ID = "delegation_id";
  public static final String FIELD_ERROR = "error";
  public static final String FIELD_REASON = "reason";
  public static final String FIELD_USAGE = "usage";
  public static final String FIELD_CONTEXT_WINDOW = "context_window";
  public static final String FIELD_SECONDS = "seconds";
  public static final String FIELD_USAGE_RATIO = "usage_ratio";
  public static final String FIELD_START_MS = "start_ms";
  public static final String FIELD_END_MS = "end_ms";
  public static final String FIELD_OFFSET_MS = "offset_ms";
  public static final String FIELD_ARGUMENTS = "arguments";
  public static final String FIELD_EVENT = "event";
  public static final String FIELD_DELTA = "delta";
}
