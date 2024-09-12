package com.alibaba.dashscope.audio.ttsv2.enrollment;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VoiceEnrollmentService {
  private final SynchronizeHalfDuplexApi<HalfDuplexServiceParam> syncApi;
  private final ApiServiceOption createServiceOptions;
  private static final String VOICE_ENROLLMENT_MODEL_NAME = "voice-enrollment";
  private String apikey;
  private String lastRequestId;
  private String model;

  public VoiceEnrollmentService(String apikey) {
    this.apikey = apikey;
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .taskGroup("audio")
            .task("tts")
            .function("customization")
            .isAsyncTask(false)
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(createServiceOptions);
    this.model = VOICE_ENROLLMENT_MODEL_NAME;
  }

  public VoiceEnrollmentService(String apikey, String model) {
    this.apikey = apikey;
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .taskGroup("audio")
            .task("tts")
            .function("customization")
            .isAsyncTask(false)
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(createServiceOptions);
    this.model = model;
  }

  /**
   * 创建新克隆音色
   *
   * @param targetModel 克隆音色对应的语音识别模型版本
   * @param prefix 音色自定义前缀，仅允许数字和小写字母，小于十个字符。
   * @param url 用于克隆的音频文件url
   * @return Voice 音色对象
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Voice createVoice(String targetModel, String prefix, String url)
      throws NoApiKeyException, InputRequiredException {
    return createVoice(
        targetModel, prefix, url, VoiceEnrollmentParam.builder().model(this.model).build());
  }

  /**
   * 查询已创建的所有音色 默认的页索引为0，默认的页大小为10
   *
   * @param prefix 音色自定义前缀，仅允许数字和小写字母，小于十个字符。可以为null。
   * @return Vocie[] 音色对象数组
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Voice[] listVoice(String prefix) throws NoApiKeyException, InputRequiredException {
    return listVoice(
        prefix, 0, 10, VoiceEnrollmentParam.builder().model(VOICE_ENROLLMENT_MODEL_NAME).build());
  }

  /**
   * 查询已创建的所有音色
   *
   * @param prefix 音色自定义前缀，仅允许数字和小写字母，小于十个字符。
   * @param pageIndex 查询的页索引
   * @param pageSize 查询的页大小
   * @return Vocie[] 音色对象数组
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Voice[] listVoice(String prefix, int pageIndex, int pageSize)
      throws NoApiKeyException, InputRequiredException {
    return listVoice(
        prefix,
        pageIndex,
        pageSize,
        VoiceEnrollmentParam.builder().model(VOICE_ENROLLMENT_MODEL_NAME).build());
  }

  /**
   * 查询指定音色
   *
   * @param voiceId 需要查询的音色
   * @return Voice 音色对象，包含状态信息和用于克隆的音频文件url
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Voice queryVoice(String voiceId) throws NoApiKeyException, InputRequiredException {
    return queryVoice(
        voiceId, VoiceEnrollmentParam.builder().model(VOICE_ENROLLMENT_MODEL_NAME).build());
  }

  /**
   * 更新音色
   *
   * @param voiceId 需要更新的音色
   * @param url 用于克隆的音频文件url
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public void updateVoice(String voiceId, String url)
      throws NoApiKeyException, InputRequiredException {
    updateVoice(
        voiceId, url, VoiceEnrollmentParam.builder().model(VOICE_ENROLLMENT_MODEL_NAME).build());
  }

  /**
   * 删除音色
   *
   * @param voiceId 需要删除的音色
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public void deleteVoice(String voiceId) throws NoApiKeyException, InputRequiredException {
    deleteVoice(voiceId, VoiceEnrollmentParam.builder().model(VOICE_ENROLLMENT_MODEL_NAME).build());
  }

  public Voice createVoice(
      String targetModel, String prefix, String url, VoiceEnrollmentParam customParam)
      throws NoApiKeyException, InputRequiredException {

    VoiceEnrollmentParam param =
        VoiceEnrollmentParam.builder()
            .operationType(VoiceEnrollmentOperationType.CREATE)
            .model(VOICE_ENROLLMENT_MODEL_NAME)
            .targetModel(targetModel)
            .prefix(prefix)
            .url(url)
            .apiKey(apikey)
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .parameters(customParam.getParameters())
            .workspace(customParam.getWorkspace())
            .build();
    param.validate();
    DashScopeResult dashScopeResult = syncApi.call(param);
    lastRequestId = dashScopeResult.getRequestId();
    return Voice.voiceFromCreateResult(dashScopeResult);
  }

  public Voice[] listVoice(
      String prefix, int pageIndex, int pageSize, VoiceEnrollmentParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VoiceEnrollmentParam param =
        VoiceEnrollmentParam.builder()
            .operationType(VoiceEnrollmentOperationType.LIST)
            .model(VOICE_ENROLLMENT_MODEL_NAME)
            .prefix(prefix)
            .pageSize(pageSize)
            .pageIndex(pageIndex)
            .apiKey(apikey)
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .parameters(customParam.getParameters())
            .workspace(customParam.getWorkspace())
            .build();
    param.validate();
    DashScopeResult dashScopeResult = syncApi.call(param);
    lastRequestId = dashScopeResult.getRequestId();
    return Voice.voiceListFromListResult(dashScopeResult);
  }

  public Voice queryVoice(String voiceId, VoiceEnrollmentParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VoiceEnrollmentParam param =
        VoiceEnrollmentParam.builder()
            .operationType(VoiceEnrollmentOperationType.QUERY)
            .model(VOICE_ENROLLMENT_MODEL_NAME)
            .voiceId(voiceId)
            .apiKey(apikey)
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .parameters(customParam.getParameters())
            .workspace(customParam.getWorkspace())
            .build();
    param.validate();
    DashScopeResult dashScopeResult = syncApi.call(param);
    lastRequestId = dashScopeResult.getRequestId();
    return Voice.voiceFromQueryResult(dashScopeResult);
  }

  public void updateVoice(String voiceId, String url, VoiceEnrollmentParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VoiceEnrollmentParam param =
        VoiceEnrollmentParam.builder()
            .operationType(VoiceEnrollmentOperationType.UPDATE)
            .model(VOICE_ENROLLMENT_MODEL_NAME)
            .voiceId(voiceId)
            .url(url)
            .apiKey(apikey)
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .parameters(customParam.getParameters())
            .workspace(customParam.getWorkspace())
            .build();
    param.validate();
    DashScopeResult dashScopeResult = syncApi.call(param);
    lastRequestId = dashScopeResult.getRequestId();
  }

  public void deleteVoice(String voiceId, VoiceEnrollmentParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VoiceEnrollmentParam param =
        VoiceEnrollmentParam.builder()
            .operationType(VoiceEnrollmentOperationType.DELETE)
            .model(VOICE_ENROLLMENT_MODEL_NAME)
            .voiceId(voiceId)
            .apiKey(apikey)
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .parameters(customParam.getParameters())
            .workspace(customParam.getWorkspace())
            .build();
    param.validate();
    DashScopeResult dashScopeResult = syncApi.call(param);
    lastRequestId = dashScopeResult.getRequestId();
  }

  /**
   * 获取最后一次请求的requestId
   *
   * @return requestId
   */
  public String getLastRequestId() {
    return lastRequestId;
  }
}
