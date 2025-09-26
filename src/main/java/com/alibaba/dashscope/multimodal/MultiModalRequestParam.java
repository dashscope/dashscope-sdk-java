package com.alibaba.dashscope.multimodal;

/**
 * author songsong.shao
 * date 2025/4/24
 */

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import io.reactivex.Flowable;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.dashscope.multimodal.MultiModalDialogApiKeyWords.*;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class MultiModalRequestParam extends FullDuplexServiceParam {
  private UpStream upStream;
  private DownStream downStream;
  private DialogAttributes dialogAttributes;
  private ClientInfo clientInfo;
  private BizParams bizParams;
  private CustomInput customInput;
  private List<Object> images;

  @Builder
  public static class CustomInput {
    private String workspaceId;
    private String appId;
    @Builder.Default private String directive = "Start";
    private String dialogId;
    @Builder.Default private String text = null;
    @Builder.Default private String type = null;
  }

  public Map<String, Object> getInputs() {
    val inputs = new HashMap<String, Object>();
    if (customInput != null) {
      inputs.put(CONST_NAME_TEXT, customInput.text);
      inputs.put(CONST_NAME_APP_ID, customInput.appId);
      inputs.put(CONST_NAME_DIRECTIVE, customInput.directive);
      inputs.put(CONST_NAME_DIALOG_ID, customInput.dialogId);
      inputs.put(CONST_NAME_TYPE, customInput.type);
      inputs.put(CONST_NAME_WORKSPACE_ID, customInput.workspaceId);
    }
    return inputs;
  }

  @Builder
  public static class UpStream {
    private String type = "AudioOnly";
    private String mode;
    private AsrPostProcessing asrPostProcessing;
    @Builder.Default private Integer sampleRate = 16000;
    private String vocabularyId = null;
    @Builder.Default private String audioFormat = CONST_AUDIO_FORMAT_PCM; //support pcm/opus
    private Map<String, Object> passThroughParams;

    @Builder
    @Setter
    public static class AsrPostProcessing {
      private List<ReplaceWord> replaceWords;

      @Builder
      @Setter
      public static class ReplaceWord {
        private String source;
        private String target;
        private String matchMode;
      }
    }
  }

  @Builder
  public static class DownStream {
    private String voice;
    private int sampleRate;
    @Builder.Default private String intermediateText = "transcript";
    @Builder.Default private boolean debug = false;
    @Builder.Default private String type = "Audio";
    @Builder.Default private int volume = 50; //0~100
    @Builder.Default private int pitchRate = 100; //50~200
    @Builder.Default private int speechRate = 100; //50~200
    @Builder.Default private String audioFormat = "pcm"; //support pcm/mp3
    private Map<String, Object> passThroughParams;
  }

  @Builder
  public static class DialogAttributes {
//    private String prompt;
  }

  @Builder
  @Setter
  public static class ClientInfo {
    private String userId;
    private Device device;
    private Network network;
    private Locations location;
    private Object status;
    private String activeForegroundApp;
    private Map<String, Object> passThroughParams;
    private String sdk;

    @Builder
    public static class Network {
      private String ip;
    }

    @Builder
    public static class Device {
      private String uuid;
    }

    @Builder
    public static class Locations {
      private String cityName;
      private String latitude;
      private String longitude;
    }
  }

  @Builder
  public static class BizParams {
    private Object userDefinedParams;
    private Object userDefinedTokens;
    private Object toolPrompts;
    private Object userQueryParams;
    private Object userPromptParams;
    private Object videos;
    private Map<String, Object> passThroughParams;
  }

  public void clearParameters() {
    upStream = null;
    downStream = null;
    clientInfo = null;
    bizParams = null;
    customInput = null;
    images = null;
    dialogAttributes = null;
  }

  @Builder
  public static class UpdateParams {
    UpStream upStream;
    DownStream downStream;
    List<Object> images;
    BizParams bizParams;
    ClientInfo clientInfo;
  }

  @Override
  public Map<String, Object> getParameters() {
    val params = new HashMap<String, Object>();
    if (upStream != null) {
      val upStreamParams = new HashMap<String, Object>();
      upStreamParams.put(CONST_NAME_UP_STREAM_TYPE, upStream.type);
      upStreamParams.put(CONST_NAME_UP_STREAM_MODE, upStream.mode);
      upStreamParams.put(CONST_NAME_UP_STREAM_AUDIO_FORMAT, upStream.audioFormat);
      if (upStream.asrPostProcessing != null){
        final var asrPostProcessingParams = getUpstreamAsrPostProcessing();
        if (!asrPostProcessingParams.isEmpty()) {
          upStreamParams.put(CONST_NAME_ASR_POST_PROCESSING, asrPostProcessingParams);
        }
      }
      upStreamParams.put(CONST_NAME_DOWN_STREAM_SAMPLE_RATE, upStream.sampleRate);
      if (upStream.vocabularyId != null) {
        upStreamParams.put(CONST_NAME_VOCABULARY_ID, upStream.vocabularyId);
      }
      if (upStream.passThroughParams != null) {
        upStreamParams.putAll(upStream.passThroughParams);
      }
      params.put(CONST_NAME_UP_STREAM, upStreamParams);
    }

    if (downStream != null) {
      val downStreamParams = new HashMap<String, Object>();
      downStreamParams.put(CONST_NAME_DOWN_STREAM_VOICE, downStream.voice);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_SAMPLE_RATE, downStream.sampleRate);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_INTERMEDIATE_TEXT, downStream.intermediateText);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_DEBUG, downStream.debug);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_TYPE, downStream.type);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_AUDIO_FORMAT, downStream.audioFormat);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_VOLUME, downStream.volume);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_PITCH_RATE, downStream.pitchRate);
      downStreamParams.put(CONST_NAME_DOWN_STREAM_SPEECH_RATE, downStream.speechRate);
      if (downStream.passThroughParams != null) {
        downStreamParams.putAll(downStream.passThroughParams);
      }
      params.put(CONST_NAME_DOWN_STREAM, downStreamParams);
    }

    if (clientInfo != null) {
      val clientInfoParams = new HashMap<String, Object>();
      clientInfoParams.put(CONST_NAME_CLIENT_INFO_USER_ID, clientInfo.userId);
      if (clientInfo.device != null) {
        val deviceParams = new HashMap<String, Object>();
        deviceParams.put(CONST_NAME_CLIENT_INFO_DEVICE_UUID, clientInfo.device.uuid);
        clientInfoParams.put(CONST_NAME_CLIENT_INFO_DEVICE, deviceParams);
      }
      if (clientInfo.network != null) {
        val networkParams = new HashMap<String, Object>();
        networkParams.put(CONST_NAME_CLIENT_INFO_NETWORK_IP, clientInfo.network.ip);
        clientInfoParams.put(CONST_NAME_CLIENT_INFO_NETWORK, networkParams);
      }
      if (clientInfo.location != null) {
        val locationParams = new HashMap<String, Object>();
        locationParams.put(CONST_NAME_CLIENT_INFO_LOCATION_CITY_NAME, clientInfo.location.cityName);
        locationParams.put(CONST_NAME_CLIENT_INFO_LOCATION_LATITUDE, clientInfo.location.latitude);
        locationParams.put(
                CONST_NAME_CLIENT_INFO_LOCATION_LONGITUDE, clientInfo.location.longitude);
        clientInfoParams.put(CONST_NAME_CLIENT_INFO_LOCATION, locationParams);
      }
      if (clientInfo.status != null) {
        clientInfoParams.put(CONST_NAME_CLIENT_INFO_STATUS, clientInfo.status);
      }
      if (clientInfo.activeForegroundApp != null){
        clientInfoParams.put(CONST_NAME_CLIENT_INFO_ACTIVE_FOREGROUND_APP, clientInfo.activeForegroundApp);
      }
      if (clientInfo.passThroughParams != null) {
        clientInfoParams.putAll(clientInfo.passThroughParams);
      }
      if (clientInfo.sdk != null){
        clientInfoParams.put(CONST_NAME_CLIENT_INFO_SDK, clientInfo.sdk);
      }
      params.put(CONST_NAME_CLIENT_INFO, clientInfoParams);
    }

    if (bizParams != null) {
      val bizParamsParams = new HashMap<String, Object>();
      if (bizParams.userDefinedParams != null) {
        bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_DEFINED_PARAMS, bizParams.userDefinedParams);
      }
      if (bizParams.userDefinedTokens != null) {
        bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_DEFINED_TOKENS, bizParams.userDefinedTokens);
      }
      if (bizParams.toolPrompts != null) {
        bizParamsParams.put(CONST_NAME_BIZ_PARAMS_TOOL_PROMPTS, bizParams.toolPrompts);
      }
      if (bizParams.userQueryParams != null) {
        bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_QUERY_PARAMS, bizParams.userQueryParams);
      }
      if (bizParams.userPromptParams != null) {
        bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_PROMPT_PARAMS, bizParams.userPromptParams);
      }
      if (bizParams.videos != null) {
        bizParamsParams.put(CONST_NAME_BIZ_PARAMS_VIDEOS, bizParams.videos);
      }
      if (bizParams.passThroughParams != null) {
        bizParamsParams.putAll(bizParams.passThroughParams);
      }
      params.put(CONST_NAME_BIZ_PARAMS, bizParamsParams);
    }

    if (images != null) {
      params.put(CONST_NAME_IMAGES, images);
    }
    return params;
  }

  private @NotNull HashMap<String, Object> getUpstreamAsrPostProcessing() {
    val asrPostProcessingParams = new HashMap<String, Object>();
    if (upStream.asrPostProcessing.replaceWords != null) {
      val replaceWords = new ArrayList<Map<String, Object>>();
      for (val replaceWord : upStream.asrPostProcessing.replaceWords) {
        val replaceWordObj= new HashMap<String, Object>();
        replaceWordObj.put(CONST_NAME_REPLACE_WORD_SOURCE, replaceWord.source);
        replaceWordObj.put(CONST_NAME_REPLACE_WORD_TARGET, replaceWord.target);
        replaceWordObj.put(CONST_NAME_REPLACE_WORD_MATCH_MODE, replaceWord.matchMode);
        replaceWords.add(replaceWordObj);
      }
      asrPostProcessingParams.put(CONST_NAME_REPLACE_WORDS, replaceWords);
    }
    return asrPostProcessingParams;
  }

  @Override
  public Flowable<Object> getStreamingData() {
    return null;
  }
}
