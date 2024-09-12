package com.alibaba.dashscope.audio.asr.vocabulary;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VocabularyService {
  private final SynchronizeHalfDuplexApi<HalfDuplexServiceParam> syncApi;
  private final ApiServiceOption createServiceOptions;
  private static final String VOCABULARY_MODEL_NAME = "speech-biasing";
  private String apikey;

  private String lastRequestId;
  private String model;

  public VocabularyService(String apikey) {
    this.apikey = apikey;
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .taskGroup("audio")
            .task("asr")
            .function("customization")
            .isAsyncTask(false)
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(createServiceOptions);
    this.model = VOCABULARY_MODEL_NAME;
  }

  public VocabularyService(String apikey, String model) {
    this.apikey = apikey;
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .taskGroup("audio")
            .task("asr")
            .function("customization")
            .isAsyncTask(false)
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(createServiceOptions);
    this.model = model;
  }

  /**
   * 创建新热词
   *
   * @param targetModel 热词对应的语音识别模型版本
   * @param prefix 热词自定义前缀，仅允许数字和小写字母，小于十个字符。
   * @param vocabulary 热词表
   * @return 热词表对象
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Vocabulary createVocabulary(String targetModel, String prefix, JsonArray vocabulary)
      throws NoApiKeyException, InputRequiredException {
    return createVocabulary(
        targetModel, prefix, vocabulary, VocabularyParam.builder().model(this.model).build());
  }

  /**
   * 查询已创建的所有热词表。默认的页索引为0，默认的页大小为10
   *
   * @param prefix 热词自定义前缀
   * @return 热词表对象数组
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Vocabulary[] listVocabulary(String prefix)
      throws NoApiKeyException, InputRequiredException {
    return listVocabulary(
        prefix, 0, 10, VocabularyParam.builder().model(VOCABULARY_MODEL_NAME).build());
  }

  /**
   * 查询已创建的所有热词表
   *
   * @param prefix 热词自定义前缀
   * @param pageIndex 查询的页索引
   * @param pageSize 查询的页大小
   * @return 热词表对象数组
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Vocabulary[] listVocabulary(String prefix, int pageIndex, int pageSize)
      throws NoApiKeyException, InputRequiredException {
    return listVocabulary(
        prefix,
        pageIndex,
        pageSize,
        VocabularyParam.builder().model(VOCABULARY_MODEL_NAME).build());
  }

  /**
   * 查询指定热词表
   *
   * @param vocabularyId 需要查询的热词表
   * @return 热词表对象
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public Vocabulary queryVocabulary(String vocabularyId)
      throws NoApiKeyException, InputRequiredException {
    return queryVocabulary(
        vocabularyId, VocabularyParam.builder().model(VOCABULARY_MODEL_NAME).build());
  }

  /**
   * 更新热词表
   *
   * @param vocabularyId 需要更新的热词表
   * @param vocabulary 热词表对象
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public void updateVocabulary(String vocabularyId, JsonArray vocabulary)
      throws NoApiKeyException, InputRequiredException {
    updateVocabulary(
        vocabularyId, vocabulary, VocabularyParam.builder().model(VOCABULARY_MODEL_NAME).build());
  }

  /**
   * 删除热词表
   *
   * @param vocabularyId 需要删除的热词表
   * @throws NoApiKeyException 如果apikey为空
   * @throws InputRequiredException 如果必须参数为空
   */
  public void deleteVocabulary(String vocabularyId)
      throws NoApiKeyException, InputRequiredException {
    deleteVocabulary(vocabularyId, VocabularyParam.builder().model(VOCABULARY_MODEL_NAME).build());
  }

  public Vocabulary createVocabulary(
      String targetModel, String prefix, JsonArray vocabulary, VocabularyParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VocabularyParam param =
        VocabularyParam.builder()
            .operationType(VocabularyOperationType.CREATE)
            .model(VOCABULARY_MODEL_NAME)
            .targetModel(targetModel)
            .prefix(prefix)
            .vocabulary(vocabulary)
            .apiKey(apikey)
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .parameters(customParam.getParameters())
            .workspace(customParam.getWorkspace())
            .build();
    param.validate();
    DashScopeResult dashScopeResult = syncApi.call(param);
    lastRequestId = dashScopeResult.getRequestId();
    return Vocabulary.vocabularyFromCreateResult(dashScopeResult);
  }

  public Vocabulary[] listVocabulary(
      String prefix, int pageIndex, int pageSize, VocabularyParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VocabularyParam param =
        VocabularyParam.builder()
            .operationType(VocabularyOperationType.LIST)
            .model(VOCABULARY_MODEL_NAME)
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
    return Vocabulary.vocabularyListFromListResult(dashScopeResult);
  }

  public Vocabulary queryVocabulary(String vocabularyId, VocabularyParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VocabularyParam param =
        VocabularyParam.builder()
            .operationType(VocabularyOperationType.QUERY)
            .model(VOCABULARY_MODEL_NAME)
            .vocabularyId(vocabularyId)
            .apiKey(apikey)
            .headers(customParam.getHeaders())
            .resources(customParam.getResources())
            .parameters(customParam.getParameters())
            .workspace(customParam.getWorkspace())
            .build();
    param.validate();
    DashScopeResult dashScopeResult = syncApi.call(param);
    lastRequestId = dashScopeResult.getRequestId();
    return Vocabulary.vocabularyFromQueryResult(dashScopeResult);
  }

  public void updateVocabulary(
      String vocabularyId, JsonArray vocabulary, VocabularyParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VocabularyParam param =
        VocabularyParam.builder()
            .operationType(VocabularyOperationType.UPDATE)
            .model(VOCABULARY_MODEL_NAME)
            .vocabularyId(vocabularyId)
            .vocabulary(vocabulary)
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

  public void deleteVocabulary(String vocabularyId, VocabularyParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VocabularyParam param =
        VocabularyParam.builder()
            .operationType(VocabularyOperationType.DELETE)
            .model(VOCABULARY_MODEL_NAME)
            .vocabularyId(vocabularyId)
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
