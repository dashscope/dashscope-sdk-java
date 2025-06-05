package com.alibaba.dashscope.multimodal;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Multimodal Dialog class responsible for handling various operations in multimodal
 * conversations.
 *
 * author songsong.shao
 * date 2025/4/24
 */
@Slf4j
public class MultiModalDialog {

  @Getter
  SynchronizeFullDuplexApi<MultiModalRequestParam> duplexApi; // Duplex communication API instance

  private ApiServiceOption serviceOption; // Service option configuration

  private Emitter<Object> conversationEmitter; // Message emitter

  private MultiModalRequestParam requestParam; // Request parameter

  private MultiModalDialogCallback callback; // Callback interface

  private MultiModalRequestParamWithStream requestParamWithStream; // Request parameter with stream

  private State.DialogState currentState =
      State.DialogState.IDLE; // Current dialogue state

  private String currentDialogId = ""; // Current dialogue ID

  @SuperBuilder
  private static class AsyncCmdBuffer { // Asynchronous command buffer class
    @Builder.Default private boolean isStop = false; // Stop flag, defaults to false
    private ByteBuffer audioFrame; // Audio frame data
    private String directive; // Directive type
  }

  private final Queue<AsyncCmdBuffer> DialogBuffer =
      new LinkedList<>(); // Dialogue buffer queue

  private AtomicReference<CountDownLatch> stopLatch =
      new AtomicReference<>(null); // Stop signal latch

  @SuperBuilder
  private static class MultiModalRequestParamWithStream
      extends MultiModalRequestParam { // Extended request parameter with stream

    @NonNull private Flowable<Object> dataStream; // Data stream

    @Override
    public Flowable<Object> getStreamingData() {
      return dataStream
          .map(
              item -> { // Map each item in the data stream
                if (item instanceof String) {
                  return JsonUtils.parse((String) item); // Parse string as JSON object
                } else if (item instanceof ByteBuffer) {
                  return item; // Return original byte buffer
                } else if (item instanceof JsonObject) {
                  return item; // Return JSON object
                } else {
                  throw new IllegalArgumentException(
                      "Unsupported type"); // Unsupported type exception
                }
              })
          .cast(Object.class); // Cast to generic object type
    }

    public static MultiModalRequestParamWithStream FromMultiModalParam(
            MultiModalRequestParam param, Flowable<Object> dataStream, String preRequestId) {

      return MultiModalRequestParamWithStream.builder()
          .parameter("pre_task_id", preRequestId)
          .headers(param.getHeaders())
          .upStream(param.getUpStream())
          .customInput(param.getCustomInput())
          .bizParams(param.getBizParams())
          .downStream(param.getDownStream())
          .clientInfo(param.getClientInfo())
          .dialogAttributes(param.getDialogAttributes())
          .images(param.getImages())
          .dataStream(dataStream)
          .customInput(param.getCustomInput())
          .model(param.getModel())
          .apiKey(param.getApiKey())
          .build();
    }
  }

  /**
   * Constructor initializes service options and creates a duplex communication API instance.
   *
   * param: param Request parameter
   * param: callback Callback interface
   */
  public MultiModalDialog(
          MultiModalRequestParam param, MultiModalDialogCallback callback) {
    this.serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AIGC.getValue())
            .task(Task.MULTIMODAL_GENERATION.getValue())
            .function(Function.GENERATION.getValue())
            .build();

    this.requestParam = param;
    this.callback = callback;
    this.duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
  }

  /**
   * Constructor allows custom service options.
   *
   * param: param Request parameter
   * param: callback Callback interface
   * param: serviceOption Custom service options
   */
  public MultiModalDialog(
      MultiModalRequestParam param,
      MultiModalDialogCallback callback,
      ApiServiceOption serviceOption) {
    this.requestParam = param;
    this.callback = callback;
    this.serviceOption = serviceOption;
    this.duplexApi = new SynchronizeFullDuplexApi<>(serviceOption);
  }

  /** Starts the dialog session. */
  public void start() {
    Flowable<Object> dataFrames =
        Flowable.create(
            emitter -> { // Creates data flow
              synchronized (
                  MultiModalDialog.this) { // Synchronized block ensures thread safety
                if (!DialogBuffer.isEmpty()) { // If dialogue buffer queue is not empty
                  for (AsyncCmdBuffer buffer :
                          DialogBuffer) { // Iterates through each buffer in the queue
                    if (buffer.isStop) { // If buffer marks stop, ends data flow
                      emitter.onComplete();
                      return;
                    } else { // Otherwise sends audio frames or directives
                      if (buffer.directive != null) {
                        emitter.onNext(buffer.directive); // Sends directive type request
                      } else {
                        emitter.onNext(buffer.audioFrame); // Sends audio frame data
                      }
                    }
                  }
                  DialogBuffer.clear(); // Clears buffer queue
                }
                conversationEmitter = emitter; // Sets emitter
              }
            },
            BackpressureStrategy.BUFFER);

    stopLatch = new AtomicReference<>(new CountDownLatch(1)); // Initializes stop signal latch

    requestParamWithStream =
        MultiModalRequestParamWithStream.FromMultiModalParam(
            this.requestParam,
            dataFrames,
            UUID.randomUUID().toString()); // Creates request parameter with stream

    try {
      this.duplexApi.duplexCall(
          requestParamWithStream,
          new ResultCallback<DashScopeResult>() { // Registers callback handler
            @Override
            public void onEvent(DashScopeResult message) {
              if (message.isBinaryOutput()) {
                callback.onSpeechAudioData(
                    (ByteBuffer) message.getOutput()); // Handles speech output data
                return;
              }

              log.info("onEvent: {}", message); // Logs event information

              if (message.getOutput() != null) {
                JsonObject output =
                    (JsonObject) message.getOutput(); // Parses output content as JSON object

                String dialogId = null;
                if (output.has("dialog_id")) {
                  dialogId = output.get("dialog_id").getAsString(); // Retrieves dialogue ID
                }

                switch (output
                    .get("event")
                    .getAsString()) { // Executes corresponding actions based on event type
                  case "Started":
                    currentDialogId = dialogId;
                    callback.onStarted(dialogId); // Dialogue start event
                    break;
                  case "Stopped":
                    callback.onStopped(dialogId); // Dialogue stop event
                    sendFinishTaskMessage();
                    break;
                  case "Error":
                    String error_code = output.has("error_code") ? output.get("error_code").getAsString() : "";
                    callback.onError(
                        dialogId,
                        error_code,
                        (output.has("error_message")
                            ? output.get("error_message").getAsString()
                            : "")); // Error event
                    break;
                  case "DialogStateChanged":
                    switch (output.get("state").getAsString()) { // Dialogue state change event
                      case "Idle":
                        currentState = State.DialogState.IDLE;
                        break;
                      case "Listening":
                        currentState = State.DialogState.LISTENING;
                        break;
                      case "Thinking":
                        currentState = State.DialogState.THINKING;
                        break;
                      case "Responding":
                        currentState = State.DialogState.RESPONDING;
                        break;
                    }
                    callback.onStateChanged(currentState); // Updates dialogue state callback
                    break;
                  case "RequestAccepted":
                    callback.onRequestAccepted(dialogId); // Request accepted event
                    break;
                  case "SpeechStarted":
                    callback.onSpeechStarted(dialogId); // Speech start event
                    break;
                  case "SpeechEnded":
                    callback.onSpeechEnded(dialogId); // Speech end event
                    break;
                  case "RespondingStarted":
                    callback.onRespondingStarted(dialogId); // Response start event
                    break;
                  case "RespondingEnded":
                    callback.onRespondingEnded(dialogId,output); // Response end event
                    break;
                  case "SpeechContent":
                    callback.onSpeechContent(dialogId, output); // Speech content event
                    break;
                  case "RespondingContent":
                    callback.onRespondingContent(dialogId, output); // Response content event
                    break;
                  default:
                    break;
                }
              }
            }

            @Override
            public void onComplete() { // Completes event handling
              if (stopLatch.get() != null) {
                stopLatch.get().countDown(); // Counts down latch
              }
              callback.onClosed(); // Closes callback
            }

            @Override
            public void onError(Exception e) { // Error event handling
              if (e instanceof ApiException) {
                ApiException apiException = (ApiException) e; // Casts exception to API exception
                if (apiException.getStatus().isJson()) {
                  callback.onError(
                          apiException.getStatus().getRequestId(),
                          apiException.getStatus().getCode(),
                          apiException.getStatus().getMessage());
                }else {
                  callback.onError(currentDialogId, "", apiException.getMessage());
                }
              }else {
                callback.onError(currentDialogId, "", e.getMessage());
              }
              if (stopLatch.get() != null) {
                stopLatch.get().countDown(); // Counts down latch
              }
            }
          });
    } catch (NoApiKeyException e) {
      ApiException apiException = new ApiException(e);
      callback.onError("", "", apiException.getMessage());
      if (stopLatch.get() != null) {
        stopLatch.get().countDown(); // Counts down latch
      }
    }
    log.debug(
        "MultiModalDialog connected, state is {}",
            currentState.getValue()); // Logs connection status
    callback.onConnected(); // Connected successfully callback
  }

  /** Starts upload speech. */
  public void startSpeech() {
    sendTextFrame("SendSpeech");
  }

  /** Stops upload speech. */
  public void stopSpeech() {
    sendTextFrame("StopSpeech");
  }

  /** Interrupts current operation. */
  public void interrupt() {
    sendTextFrame("RequestToSpeak");
  }

  /** Local player start broadcast tts */
  public void localRespondingStarted() {
    sendTextFrame("LocalRespondingStarted");
  }

  /** Local player broadcast tts end */
  public void localRespondingEnded() {
    sendTextFrame("LocalRespondingEnded");
  }

//  /** Requests to speak. */
//  public void requestToSpeak() {
//    sendTextFrame("RequestToSpeak");
//  }

  /**
   * Requests response.
   *
   * param: type Response type
   * param: text Response text
   * param: updateParams Update parameters
   */
  public void requestToRespond(
      String type, String text, MultiModalRequestParam.UpdateParams updateParams) {
    requestParamWithStream.clearParameters();
    MultiModalRequestParam.CustomInput customInput =
        MultiModalRequestParam.CustomInput.builder()
            .directive("RequestToRespond")
            .dialogId(currentDialogId)
            .type(type)
            .text(text)
            .build();
    requestParamWithStream.setCustomInput(customInput);
    if (updateParams != null && updateParams.images != null) {
      requestParamWithStream.setImages(updateParams.images);
    }
    if (updateParams != null && updateParams.bizParams != null) {
      requestParamWithStream.setBizParams(updateParams.bizParams);
    }
    sendTextFrame("RequestToRespond");
  }

  /**
   * Updates information.
   *
   * param: updateParams Update parameters
   */
  public void updateInfo(MultiModalRequestParam.UpdateParams updateParams) {
    requestParamWithStream.clearParameters();
    MultiModalRequestParam.CustomInput customInput =
            MultiModalRequestParam.CustomInput.builder()
                    .directive("UpdateInfo")
                    .dialogId(currentDialogId)
                    .build();
    requestParamWithStream.setCustomInput(customInput);
    if (updateParams != null && updateParams.clientInfo != null) {
      requestParamWithStream.setClientInfo(updateParams.clientInfo);
    }
    if (updateParams != null && updateParams.bizParams != null) {
      requestParamWithStream.setBizParams(updateParams.bizParams);
    }
    if (updateParams != null && updateParams.images != null) {
      requestParamWithStream.setImages(updateParams.images);
    }
    sendTextFrame("UpdateInfo");
  }

  /** Stops the MultiModalDialog. */
  public void stop() {
    sendFinishTaskMessage();
    if (stopLatch.get() != null) {
      try {
        stopLatch.get().await();
      } catch (InterruptedException ignored) {
      }
    }
  }

  /**
   * Gets current dialogue state.
   *
   * return: Current dialogue state
   */
  public State.DialogState getDialogState() {
    return currentState;
  }

//  /** Gets dialogue mode. */
//  public void getDialogMode() {
//    //
//  }

  /**
   * Sends audio frame.
   *
   * param: audioFrame Audio frame data
   */
  public void sendAudioData(ByteBuffer audioFrame) {
    if (audioFrame == null) {
      throw new ApiException(
          new InputRequiredException(
              "Parameter invalid: audioFrame is null")); // Invalid parameter exception
    }
    synchronized (MultiModalDialog.this) { // Synchronized block ensures thread safety
      if (conversationEmitter != null) {
        log.debug(
            "submitAudioFrame to new emitter: "
                + audioFrame); // Logs submission of audio frame information
        conversationEmitter.onNext(audioFrame); // Sends audio frame data
      } else {
        DialogBuffer.add(
            AsyncCmdBuffer.builder().audioFrame(audioFrame).build()); // Adds to buffer queue
      }
    }
  }

  /**
   * Sends text frame.
   *
   * param: textFrame Text frame data
   */
  private void sendTextFrame(
      String textFrame) { // Instruction type FullDuplex.getWebSocketPayload(data)
    if (Objects.equals(textFrame, "")) {
      throw new ApiException(
          new InputRequiredException(
              "Parameter invalid: text is null")); // Invalid parameter exception
    }
    synchronized (MultiModalDialog.this) { // Synchronized block ensures thread safety
      if (conversationEmitter != null) {
        log.debug(
            "submitText to new emitter: {}",
            textFrame); // Logs submission of text frame information

        if ("RequestToRespond".equals(textFrame) || "UpdateInfo".equals(textFrame)) {
          // RequestToRespond and UpdateInfo have special inputs and parameters
          conversationEmitter.onNext(
              JsonUtils.toJson(requestParamWithStream.getCustomInput())); // Sends custom input data
        } else {
          log.debug("clear parameters"); // Clears parameters logs
          requestParamWithStream.clearParameters();

          JsonObject jsonObject = new JsonObject(); // Creates JSON object
          jsonObject.addProperty("directive", textFrame); // Adds directive property
          jsonObject.addProperty("dialog_id", currentDialogId); // Adds dialogue ID property
          conversationEmitter.onNext(jsonObject); // Sends JSON object data
        }
      } else {
        JsonObject jsonObject = new JsonObject(); // Creates JSON object
        jsonObject.addProperty("directive", textFrame); // Adds directive property
        jsonObject.addProperty("dialog_id", currentDialogId); // Adds dialogue ID property
        DialogBuffer.add(
            AsyncCmdBuffer.builder()
                .directive(JsonUtils.toJson(jsonObject))
                .build()); // Adds to buffer queue
      }
    }
  }

  /** Sends stop message. */
  private void sendFinishTaskMessage() { // Instruction type
    synchronized (MultiModalDialog.this) { // Synchronized block ensures thread safety
      if (conversationEmitter != null) {
        conversationEmitter.onComplete(); // Ends data flow
      }
    }
  }
}
