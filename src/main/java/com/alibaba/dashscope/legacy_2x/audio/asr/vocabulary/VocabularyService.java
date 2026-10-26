package com.alibaba.dashscope.legacy_2x.audio.asr.vocabulary;

import com.alibaba.dashscope.legacy_2x.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.legacy_2x.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.protocol.ApiServiceOption;
import com.alibaba.dashscope.legacy_2x.protocol.HttpMethod;
import com.alibaba.dashscope.legacy_2x.protocol.Protocol;
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
   * Creates a vocabulary.
   *
   * @param targetModel Speech recognition model version associated with the vocabulary
   * @param prefix Custom vocabulary prefix; digits and lowercase letters only, fewer than 10
   *     characters
   * @param vocabulary Vocabulary entries
   * @return Vocabulary object
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Vocabulary createVocabulary(String targetModel, String prefix, JsonArray vocabulary)
      throws NoApiKeyException, InputRequiredException {
    return createVocabulary(
        targetModel, prefix, vocabulary, VocabularyParam.builder().model(this.model).build());
  }

  /**
   * Lists all vocabularies. The default page index is 0, and the default page size is 10.
   *
   * @param prefix Custom vocabulary prefix
   * @return Array of vocabulary objects
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Vocabulary[] listVocabulary(String prefix)
      throws NoApiKeyException, InputRequiredException {
    return listVocabulary(prefix, 0, 10, VocabularyParam.builder().model(this.model).build());
  }

  /**
   * Lists all vocabularies.
   *
   * @param prefix Custom vocabulary prefix
   * @param pageIndex Page index to query
   * @param pageSize Page size to query
   * @return Array of vocabulary objects
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Vocabulary[] listVocabulary(String prefix, int pageIndex, int pageSize)
      throws NoApiKeyException, InputRequiredException {
    return listVocabulary(
        prefix, pageIndex, pageSize, VocabularyParam.builder().model(this.model).build());
  }

  /**
   * Queries a vocabulary.
   *
   * @param vocabularyId Vocabulary to query
   * @return Vocabulary object
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public Vocabulary queryVocabulary(String vocabularyId)
      throws NoApiKeyException, InputRequiredException {
    return queryVocabulary(vocabularyId, VocabularyParam.builder().model(this.model).build());
  }

  /**
   * Updates a vocabulary.
   *
   * @param vocabularyId Vocabulary to update
   * @param vocabulary Vocabulary object
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public void updateVocabulary(String vocabularyId, JsonArray vocabulary)
      throws NoApiKeyException, InputRequiredException {
    updateVocabulary(vocabularyId, vocabulary, VocabularyParam.builder().model(this.model).build());
  }

  /**
   * Deletes a vocabulary.
   *
   * @param vocabularyId Vocabulary to delete
   * @throws NoApiKeyException If apikey is empty
   * @throws InputRequiredException If a required parameter is empty
   */
  public void deleteVocabulary(String vocabularyId)
      throws NoApiKeyException, InputRequiredException {
    deleteVocabulary(vocabularyId, VocabularyParam.builder().model(this.model).build());
  }

  public Vocabulary createVocabulary(
      String targetModel, String prefix, JsonArray vocabulary, VocabularyParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VocabularyParam param =
        VocabularyParam.builder()
            .operationType(VocabularyOperationType.CREATE)
            .model(this.model)
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
    return Vocabulary.vocabularyListFromListResult(dashScopeResult);
  }

  public Vocabulary queryVocabulary(String vocabularyId, VocabularyParam customParam)
      throws NoApiKeyException, InputRequiredException {
    VocabularyParam param =
        VocabularyParam.builder()
            .operationType(VocabularyOperationType.QUERY)
            .model(this.model)
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
            .model(this.model)
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
            .model(this.model)
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
   * Gets the requestId of the last request.
   *
   * @return requestId
   */
  public String getLastRequestId() {
    return lastRequestId;
  }
}
