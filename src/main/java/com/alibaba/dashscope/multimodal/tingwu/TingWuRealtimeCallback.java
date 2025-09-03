package com.alibaba.dashscope.multimodal.tingwu;

import com.google.gson.JsonObject;

/**
 * Abstract class representing callbacks for multi-modal conversation events.
 *
 * author songsong.shao
 * date 2025/4/27
 */
public abstract class TingWuRealtimeCallback {

  /**
   * Called when a conversation starts with a specific dialog ID.
   */
  public abstract void onStarted(String taskId);

  /**
   * Called when a conversation stops with a specific dialog ID.
   */
  public abstract void onStopped(String taskId);

  /**
   * Called when an error occurs during a conversation.
   *
   * param errorCode The error code associated with the error.
   * param errorMsg The error message associated with the error.
   */
  public abstract void onError(String errorCode, String errorMsg);

  /**
   * Called when responding content is available in a specific dialog.
   *
   * param taskId The unique identifier for the dialog.
   * param content The content of the response as a JsonObject.
   */
  public abstract void onAiResult(String taskId, JsonObject content);

  /**
   * Called when speech content is available in a specific dialog.
   *
   * param taskId The unique identifier for the dialog.
   * param content The content of the speech as a JsonObject.
   */
  public abstract void onRecognizeResult(String taskId, JsonObject content);

  /**
   * Called when a request is accepted in a specific dialog.
   *
   * param taskId The unique identifier for the dialog.
   * param dataId for this task
   */
  public abstract void onSpeechListen(String taskId, String dataId);

  /** Called when the conversation closes. */
  public abstract void onClosed();
}
