package com.alibaba.dashscope.legacy_2x.audio.ttsv2.enrollment;

import com.alibaba.dashscope.legacy_2x.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.legacy_2x.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.protocol.ApiServiceOption;
import com.alibaba.dashscope.legacy_2x.protocol.HttpMethod;
import com.alibaba.dashscope.legacy_2x.protocol.Protocol;
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
   * Creates a cloned voice.
   *
   * @param targetModel Speech recognition model version associated with the cloned voice
   * @param prefix Custom voice prefix; digits and lowercase letters only, fewer than 10 characters
   * @param url URL of the audio file used for cloning
   * @return Voice Voice object
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Voice createVoice(String targetModel, String prefix, String url)
      throws NoApiKeyException, InputRequiredException {
    return createVoice(
        targetModel, prefix, url, VoiceEnrollmentParam.builder().model(this.model).build());
  }

  /**
   * Lists all voices. The default page index is 0, and the default page size is 10.
   *
   * @param prefix Custom voice prefix; digits and lowercase letters only, fewer than 10 characters.
   *     May be null.
   * @return Vocie[] Array of voice objects
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Voice[] listVoice(String prefix) throws NoApiKeyException, InputRequiredException {
    return listVoice(prefix, 0, 10, VoiceEnrollmentParam.builder().model(this.model).build());
  }

  /**
   * Lists all voices.
   *
   * @param prefix Custom voice prefix; digits and lowercase letters only, fewer than 10 characters
   * @param pageIndex Page index to query
   * @param pageSize Page size to query
   * @return Vocie[] Array of voice objects
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Voice[] listVoice(String prefix, int pageIndex, int pageSize)
      throws NoApiKeyException, InputRequiredException {
    return listVoice(
        prefix, pageIndex, pageSize, VoiceEnrollmentParam.builder().model(this.model).build());
  }

  /**
   * Queries a voice.
   *
   * @param voiceId Voice to query
   * @return Voice Voice object containing status information and the URL of the audio file used for
   *     cloning
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Voice queryVoice(String voiceId) throws NoApiKeyException, InputRequiredException {
    return queryVoice(voiceId, VoiceEnrollmentParam.builder().model(this.model).build());
  }

  /**
   * Updates a voice.
   *
   * @param voiceId Voice to update
   * @param url URL of the audio file used for cloning
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public void updateVoice(String voiceId, String url)
      throws NoApiKeyException, InputRequiredException {
    updateVoice(voiceId, url, VoiceEnrollmentParam.builder().model(this.model).build());
  }

  /**
   * Deletes a voice.
   *
   * @param voiceId Voice to delete
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public void deleteVoice(String voiceId) throws NoApiKeyException, InputRequiredException {
    deleteVoice(voiceId, VoiceEnrollmentParam.builder().model(this.model).build());
  }

  public Voice createVoice(
      String targetModel, String prefix, String url, VoiceEnrollmentParam customParam)
      throws NoApiKeyException, InputRequiredException {

    VoiceEnrollmentParam param =
        VoiceEnrollmentParam.builder()
            .operationType(VoiceEnrollmentOperationType.CREATE)
            .model(customParam.getModel().isEmpty() ? this.model : customParam.getModel())
            .targetModel(targetModel)
            .prefix(prefix)
            .url(url)
            .apiKey(apikey)
            .languageHints(customParam.getLanguageHints())
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .maxPromptAudioLength(customParam.getMaxPromptAudioLength())
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
            .model(this.model)
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
            .model(this.model)
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
            .model(this.model)
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
            .model(this.model)
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
   * Gets the requestId of the last request.
   *
   * @return requestId
   */
  public String getLastRequestId() {
    return lastRequestId;
  }
}
