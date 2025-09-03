// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.imagesynthesis;

import com.alibaba.dashscope.api.AsynchronousApi;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.task.AsyncTaskListParam;

public final class ImageSynthesis {
  /** default task, function & taskGroup */
  private String task = "text2image";

  private final String function = "image-synthesis";
  private final String taskGroup = "aigc";

  private final AsynchronousApi<HalfDuplexServiceParam> asyncApi;
  private final GeneralApi<HalfDuplexServiceParam> syncApi;
  private final ApiServiceOption createServiceOptions;
  private final String baseUrl;

  public static class Models {
    public static final String WANX_V1 = "wanx-v1";
    public static final String WANX_SKETCH_TO_IMAGE_V1 = "wanx-sketch-to-image-v1";

    public static final String WANX_2_1_IMAGEEDIT = "wanx2.1-imageedit";
  }

  /** Image edit function */
  public static class ImageEditFunction {
    public static final String STYLIZATION_ALL = "stylization_all";
    public static final String STYLIZATION_LOCAL = "stylization_local";
    public static final String DESCRIPTION_EDIT = "description_edit";
    public static final String DESCRIPTION_EDIT_WITH_MASK = "description_edit_with_mask";
    public static final String DOODLE = "doodle";
    public static final String REMOVE_WATERMAKER = "remove_watermaker";
    public static final String EXPAND = "expand";
    public static final String SUPER_RESOLUTION = "super_resolution";
    public static final String COLORIZATION = "colorization";
  }

  /** Default test2image */
  public ImageSynthesis() {
    // only support http
    asyncApi = new AsynchronousApi<HalfDuplexServiceParam>();
    syncApi = new GeneralApi<HalfDuplexServiceParam>();
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.NONE)
            .taskGroup(taskGroup)
            .task(task)
            .function(function)
            .isAsyncTask(true)
            .build();
    this.baseUrl = null;
  }

  /**
   * The task of the image synthesis image2image or test2image.
   *
   * @param task The task of image synthesis(image2image|text2image).
   */
  public ImageSynthesis(String task) {
    // only support http
    asyncApi = new AsynchronousApi<HalfDuplexServiceParam>();
    syncApi = new GeneralApi<HalfDuplexServiceParam>();
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.NONE)
            .taskGroup(taskGroup)
            .task(task)
            .function(function)
            .isAsyncTask(true)
            .build();
    this.baseUrl = null;
  }

  /**
   * Create with task and custom baseUrl
   *
   * @param task The task of image synthesis(image2image|text2image).
   * @param baseUrl The service base url.
   */
  public ImageSynthesis(String task, String baseUrl) {
    // only support http
    asyncApi = new AsynchronousApi<HalfDuplexServiceParam>();
    syncApi = new GeneralApi<HalfDuplexServiceParam>();
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .baseHttpUrl(baseUrl)
            .streamingMode(StreamingMode.NONE)
            .taskGroup(taskGroup)
            .task(task)
            .function(function)
            .isAsyncTask(true)
            .build();
    this.baseUrl = baseUrl;
  }

  public ImageSynthesisResult asyncCall(ImageSynthesisParam param)
      throws ApiException, NoApiKeyException {
    // add local file support
    try {
      param.checkAndUpload();
    }catch (UploadFileException e){
      throw new ApiException(e);
    }
    ApiServiceOption serviceOption = createServiceOptions;
    if (param.getModel().contains("imageedit")) {
      serviceOption.setTask("image2image");
    }
    return ImageSynthesisResult.fromDashScopeResult(
        asyncApi.asyncCall(param, serviceOption));
  }

  /**
   *  Note: This method currently now only supports wan2.2-t2i-flash and wan2.2-t2i-plus.
   *    Using other models will result in an error，More raw image models may be added for use later
   */
  public ImageSynthesisResult syncCall(ImageSynthesisParam param)
          throws ApiException, NoApiKeyException {
    // add local file support
    try {
      param.checkAndUpload();
    }catch (UploadFileException e){
      throw new ApiException(e);
    }
    ApiServiceOption serviceOption = createServiceOptions;
    serviceOption.setIsAsyncTask(false);
    return ImageSynthesisResult.fromDashScopeResult(
            syncApi.call(param, serviceOption));
  }

  /**
   * Call the server to get the result.
   *
   * @param param The input param of class `ImageSynthesisParam`.
   * @return The image synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public ImageSynthesisResult call(ImageSynthesisParam param)
      throws ApiException, NoApiKeyException {
    // add local file support
    try {
      param.checkAndUpload();
    }catch (UploadFileException e){
      throw new ApiException(e);
    }
    ApiServiceOption serviceOption = createServiceOptions;
    if (param.getModel().contains("imageedit")) {
      serviceOption.setTask("image2image");
    }
    return ImageSynthesisResult.fromDashScopeResult(
            asyncApi.call(param, serviceOption));
  }

  /**
   * @param param The input param of class `SketchImageSynthesisParam`
   * @return The image synthesis result `ImageSynthesisResult`
   * @throws ApiException The dashscope exception.
   * @throws NoApiKeyException No api key provide.
   */
  public ImageSynthesisResult asyncCall(SketchImageSynthesisParam param)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisResult.fromDashScopeResult(
        asyncApi.asyncCall(param, createServiceOptions));
  }

  /**
   * Call the server to get the result.
   *
   * @param param The input param of class `SketchImageSynthesisParam`.
   * @return The image synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public ImageSynthesisResult call(SketchImageSynthesisParam param)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisResult.fromDashScopeResult(asyncApi.call(param, createServiceOptions));
  }

  public ImageSynthesisListResult list(AsyncTaskListParam param)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisListResult.fromDashScopeResult(asyncApi.list(param, baseUrl));
  }

  public ImageSynthesisListResult list(
      String startTime,
      String endTime,
      String modelName,
      String apiKeyId,
      String region,
      String status,
      Integer pageNo,
      Integer pageSize)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisListResult.fromDashScopeResult(
        asyncApi.list(
            startTime, endTime, modelName, apiKeyId, region, status, pageNo, pageSize, baseUrl));
  }

  public ImageSynthesisResult fetch(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisResult.fromDashScopeResult(asyncApi.fetch(taskId, apiKey, baseUrl));
  }

  public ImageSynthesisResult fetch(ImageSynthesisResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {

    return ImageSynthesisResult.fromDashScopeResult(
        asyncApi.fetch(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }

  public ImageSynthesisResult cancel(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisResult.fromDashScopeResult(asyncApi.cancel(taskId, apiKey, baseUrl));
  }

  public ImageSynthesisResult cancel(ImageSynthesisResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    DashScopeResult res = asyncApi.cancel(taskInfo.getOutput().getTaskId(), apiKey, baseUrl);
    return ImageSynthesisResult.fromDashScopeResult(res);
  }

  public ImageSynthesisResult wait(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisResult.fromDashScopeResult(asyncApi.wait(taskId, apiKey, baseUrl));
  }

  public ImageSynthesisResult wait(ImageSynthesisResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageSynthesisResult.fromDashScopeResult(
        asyncApi.wait(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }
}
