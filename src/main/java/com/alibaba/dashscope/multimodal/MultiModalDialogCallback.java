package com.alibaba.dashscope.multimodal;

import com.google.gson.JsonObject;

import java.nio.ByteBuffer;

/**
 * Abstract class representing callbacks for multi-modal conversation events.
 *
 * author songsong.shao
 * date 2025/4/27
 */
public abstract class MultiModalDialogCallback {

  /** Called when the conversation is connected. */
  public abstract void onConnected();

  /**
   * Called when a conversation starts with a specific dialog ID.
   */
  public abstract void onStarted(String dialogId);

  /**
   * Called when a conversation stops with a specific dialog ID.
   */
  public abstract void onStopped(String dialogId);

  /**
   * Called when speech starts in a specific dialog.
   */
  public abstract void onSpeechStarted(String dialogId);

  /**
   * Called when speech ends in a specific dialog.
   */
  public abstract void onSpeechEnded(String dialogId);

  /**
   * Called when an error occurs during a conversation.
   *
   * param dialogId The unique identifier for the dialog.
   * param errorCode The error code associated with the error.
   * param errorMsg The error message associated with the error.
   */
  public abstract void onError(String dialogId, String errorCode, String errorMsg);

  /**
   * Called when the conversation state changes.
   *
   * param state The new state of the conversation.
   */
  public abstract void onStateChanged(State.DialogState state);

  /**
   * Called when speech audio data is available.
   *
   * param audioData The audio data as a ByteBuffer.
   */
  public abstract void onSpeechAudioData(ByteBuffer audioData);

  /**
   * Called when responding starts in a specific dialog.
   *
   * param dialogId The unique identifier for the dialog.
   */
  public abstract void onRespondingStarted(String dialogId);

  /**
   * Called when responding ends in a specific dialog.
   *
   * param dialogId The unique identifier for the dialog.
   * param content The content of the response as a JsonObject.
   */
  public abstract void onRespondingEnded(String dialogId, JsonObject content);

  /**
   * Called when responding content is available in a specific dialog.
   *
   * param dialogId The unique identifier for the dialog.
   * param content The content of the response as a JsonObject.
   */
  public abstract void onRespondingContent(String dialogId, JsonObject content);

  /**
   * Called when speech content is available in a specific dialog.
   *
   * param dialogId The unique identifier for the dialog.
   * param content The content of the speech as a JsonObject.
   */
  public abstract void onSpeechContent(String dialogId, JsonObject content);

  /**
   * Called when a request is accepted in a specific dialog.
   *
   * param dialogId The unique identifier for the dialog.
   */
  public abstract void onRequestAccepted(String dialogId);

  /** Called when the conversation closes. */
  public abstract void onClosed();
}
