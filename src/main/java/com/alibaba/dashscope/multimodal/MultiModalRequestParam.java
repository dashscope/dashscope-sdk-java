package com.alibaba.dashscope.multimodal;

/**
 * author songsong.shao
 * date 2025/4/24
 */

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.val;

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
    //    private int sampleRate;
    @Builder.Default private String audioFormat = CONST_AUDIO_FORMAT_PCM; //support pcm/opus
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
  }

  @Builder
  public static class DialogAttributes {
//    private String prompt;
  }

  @Builder
  public static class ClientInfo {
    private String userId;
    private Device device;
    private Network network;
    private Locations location;
    private Object status;

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
      params.put(CONST_NAME_DOWN_STREAM, downStreamParams);
    }

    if (clientInfo != null) {
      val clientInfoParams = new HashMap<String, Object>();
      clientInfoParams.put(CONST_NAME_CLIENT_INFO_USER_ID, clientInfo.userId);
      if (clientInfo.device != null) {
        val deviceParams = new HashMap<String, Object>();
        deviceParams.put(CONST_NAME_CLIENT_INFO_DEVICE_UUID, clientInfo.device);
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
      params.put(CONST_NAME_CLIENT_INFO, clientInfoParams);
    }

    if (bizParams != null) {
      val bizParamsParams = new HashMap<String, Object>();
      bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_DEFINED_PARAMS, bizParams.userDefinedParams);
      bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_DEFINED_TOKENS, bizParams.userDefinedTokens);
      bizParamsParams.put(CONST_NAME_BIZ_PARAMS_TOOL_PROMPTS, bizParams.toolPrompts);
      bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_QUERY_PARAMS, bizParams.userQueryParams);
      bizParamsParams.put(CONST_NAME_BIZ_PARAMS_USER_PROMPT_PARAMS, bizParams.userPromptParams);
      params.put(CONST_NAME_BIZ_PARAMS, bizParamsParams);
    }

    if (images != null) {
      params.put(CONST_NAME_IMAGES, images);
    }
    return params;
  }

  @Override
  public Flowable<Object> getStreamingData() {
    return null;
  }
}
