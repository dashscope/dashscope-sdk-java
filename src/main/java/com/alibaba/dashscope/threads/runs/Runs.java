package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.common.GeneralListParam;
import com.alibaba.dashscope.common.ListResult;
import com.alibaba.dashscope.common.UpdateMetadataParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.InvalidateParameter;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.google.gson.reflect.TypeToken;
import io.reactivex.Flowable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class Runs {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .path("assistants")
        .build();
  }

  public Runs() {
    serviceOption = defaultServiceOption();
    api = new GeneralApi<>();
  }

  public Runs(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
  }

  /**
   * Create a thread run.
   *
   * @param threadId The thread id.
   * @param param The `RunParam`
   * @return The `Run` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InputRequiredException The threadId must input.
   * @throws InvalidateParameter The input parameter is invalid.
   */
  public Run create(String threadId, RunParam param)
      throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter {
    if (threadId == null || threadId.equals("")) {
      throw new InputRequiredException("threadId is required!");
    }
    if (param.getStream()) {
      throw new InvalidateParameter("Request with stream=true should use createStream");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/runs", threadId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Run.class);
  }

  /**
   * Create stream run, return Flowable of StreamMessage.
   *
   * @param threadId The run thread id
   * @param param The run parameters.
   * @return The StreamMessage flowable.
   * @throws ApiException The request failed.
   * @throws NoApiKeyException No api key found.
   * @throws InputRequiredException The threadId is null or empty.
   * @throws InvalidateParameter The stream in param is false.
   */
  public Flowable<AssistantStreamMessage> createStream(String threadId, RunParam param)
      throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter {
    if (threadId == null || threadId.equals("")) {
      throw new InputRequiredException("threadId is required!");
    }
    if (!param.getStream()) {
      throw new InvalidateParameter("Request with stream=false should use create");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/runs", threadId));
    Flowable<DashScopeResult> result = api.streamCall(param, serviceOption);
    return result.map(
        item -> FlattenResultBase.fromDashScopeResult(item, AssistantStreamMessage.class, true));
  }

  /**
   * Create stream run, with event handler.
   *
   * @param threadId The run thread id
   * @param param The run parameters.
   * @param handler The assistant event handler.
   * @throws ApiException The request failed.
   * @throws NoApiKeyException No api key found.
   * @throws InputRequiredException The threadId is null or empty.
   * @throws InvalidateParameter The stream in param is false.
   */
  public void createStream(String threadId, RunParam param, AssistantEventHandler handler)
      throws ApiException, NoApiKeyException, InputRequiredException, InvalidateParameter {
    if (threadId == null || threadId.equals("")) {
      throw new InputRequiredException("threadId is required!");
    }
    if (!param.getStream()) {
      throw new InvalidateParameter("Request with stream=false should use create");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/runs", threadId));

    api.streamCall(param, serviceOption, new StreamEventProcessingCallback(handler));
  }

  /**
   * Create thread and run.
   *
   * @param param The `ThreadAndRunParam` object.
   * @return The `Run` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InvalidateParameter The input parameter is invalid.
   */
  public Run createThreadAndRun(ThreadAndRunParam param)
      throws ApiException, NoApiKeyException, InvalidateParameter {
    if (param.getStream()) {
      throw new InvalidateParameter("Request with stream=false should use create");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/runs"));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Run.class);
  }

  /**
   * Create stream output and run.
   *
   * @param param The `ThreadAndRunParam` object with stream=true.
   * @return Flowable of AssistantStreamMessage you can iterator the result.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InvalidateParameter The input parameter is invalid.
   */
  public Flowable<AssistantStreamMessage> createStreamThreadAndRun(ThreadAndRunParam param)
      throws ApiException, NoApiKeyException, InvalidateParameter {
    if (!param.getStream()) {
      throw new InvalidateParameter("Request with stream=false should use create");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/runs"));
    return api.streamCall(param, serviceOption)
        .map(
            dashscopeResult ->
                FlattenResultBase.fromDashScopeResult(
                    dashscopeResult, AssistantStreamMessage.class, true));
  }

  /**
   * Create stream output and run.
   *
   * @param param The `ThreadAndRunParam` object with stream=true.
   * @param handler The stream event handler callback.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InvalidateParameter The input parameter is invalid.
   */
  public void createStreamThreadAndRun(ThreadAndRunParam param, AssistantEventHandler handler)
      throws ApiException, NoApiKeyException, InvalidateParameter {
    if (!param.getStream()) {
      throw new InvalidateParameter("Request with stream=false should use create");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/runs"));
    api.streamCall(param, serviceOption, new StreamEventProcessingCallback(handler));
  }

  /**
   * Update the run.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @param param The parameter.
   * @return The updated `Run` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InputRequiredException The thread id and run id are required.
   */
  public Run update(String threadId, String runId, UpdateMetadataParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.equals("") || runId == null || runId.isEmpty()) {
      throw new InputRequiredException("threadId and runId are required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/runs/%s", threadId, runId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Run.class);
  }

  /**
   * List the run of thread id.
   *
   * @param threadId the thread id.
   * @param listParam The list parameter.
   * @return The list of `Run`.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   */
  public ListResult<Run> list(String threadId, GeneralListParam listParam)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s/runs", threadId));
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<Run>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }

  /**
   * List the run step of the run id.
   *
   * @param threadId The thread id
   * @param runId The run id.
   * @param listParam The list parameter.
   * @return The list of 'RunStep'
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   */
  public ListResult<RunStep> listSteps(String threadId, String runId, GeneralListParam listParam)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s/runs/%s/steps", threadId, runId));
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<RunStep>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }

  /**
   * Retrieve the run.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @return The `Run` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   */
  public Run retrieve(String threadId, String runId) throws ApiException, NoApiKeyException {
    return retrieve(threadId, runId, null);
  }

  /**
   * Retrieve the run.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @param apiKey The request api key.
   * @return The `Run` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   */
  public Run retrieve(String threadId, String runId, String apiKey)
      throws ApiException, NoApiKeyException {
    return retrieve(threadId, runId, apiKey, new HashMap<>());
  }

  public Run retrieve(String threadId, String runId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s/runs/%s", threadId, runId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Run.class);
  }

  /**
   * Retrieve the run step.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @param stepId the run step id.
   * @return The `RunStep` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   */
  public RunStep retrieveStep(String threadId, String runId, String stepId)
      throws ApiException, NoApiKeyException {
    return retrieveStep(threadId, runId, stepId, null);
  }

  /**
   * Retrieve the run step.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @param stepId the run step id.
   * @param apiKey The request api key.
   * @return The `RunStep` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   */
  public RunStep retrieveStep(String threadId, String runId, String stepId, String apiKey)
      throws ApiException, NoApiKeyException {
    return retrieveStep(threadId, runId, stepId, apiKey, new HashMap<>());
  }

  public RunStep retrieveStep(
      String threadId, String runId, String stepId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s/runs/%s/steps/%s", threadId, runId, stepId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, RunStep.class);
  }

  /**
   * Submit tool outputs.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @param param The output parameters.
   * @return The `Run` object.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InputRequiredException The thread id and run id must input.
   */
  public Run submitToolOutputs(String threadId, String runId, SubmitToolOutputsParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.equals("") || runId == null || runId.isEmpty()) {
      throw new InputRequiredException("threadId and runId are required!");
    }
    param.validate();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/runs/%s/submit_tool_outputs", threadId, runId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Run.class);
  }

  /**
   * Submit tool outputs, and return stream result.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @param param The output parameters.
   * @return The stream assistant stream messages.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InputRequiredException The thread id and run id must input.
   */
  public Flowable<AssistantStreamMessage> submitStreamToolOutputs(
      String threadId, String runId, SubmitToolOutputsParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.equals("") || runId == null || runId.isEmpty()) {
      throw new InputRequiredException("threadId and runId are required!");
    }
    param.validate();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/runs/%s/submit_tool_outputs", threadId, runId));
    return api.streamCall(param, serviceOption)
        .map(
            dashscopeResult ->
                FlattenResultBase.fromDashScopeResult(
                    dashscopeResult, AssistantStreamMessage.class, true));
  }

  /**
   * Submit tool outputs, and return stream result.
   *
   * @param threadId The thread id.
   * @param runId The run id.
   * @param param The output parameters.
   * @param handler The stream event handler callback.
   * @throws ApiException The request exception, if network connection issue etc.
   * @throws NoApiKeyException Can not find a valid api key
   * @throws InputRequiredException The thread id and run id must input.
   */
  public void submitStreamToolOutputs(
      String threadId, String runId, SubmitToolOutputsParam param, AssistantEventHandler handler)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.equals("") || runId == null || runId.isEmpty()) {
      throw new InputRequiredException("threadId and runId are required!");
    }
    param.validate();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/runs/%s/submit_tool_outputs", threadId, runId));
    api.streamCall(param, serviceOption, new StreamEventProcessingCallback(handler));
  }
}
