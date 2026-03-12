// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.videosynthesis;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.GsonExclude;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.PreprocessInputImage;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class VideoSynthesisParam extends HalfDuplexServiceParam {

  @Data
  @SuperBuilder
  public static class Media {
    @Builder.Default private String url = null;
    @Builder.Default private String type = null;
    @Builder.Default private String referenceVoice = null;
    @Builder.Default private String referenceDescription = null;
  }

  @Builder.Default private Map<String, String> inputChecks = new HashMap<>();

  @Builder.Default private String size = null;

  @Builder.Default private Integer steps = null;

  @Builder.Default private Integer seed = null;

  @Builder.Default private String prompt = null;

  /** The negative prompt is the opposite of the prompt meaning. use negativePrompt */
  @Deprecated @Builder.Default private String negative_prompt = null;

  /** The negative prompt is the opposite of the prompt meaning. */
  @Builder.Default private String negativePrompt = null;

  /** LoRa input, such as gufeng, katong, etc. */
  @Builder.Default private String template = null;

  /** use promptExtend in parameters */
  @Deprecated @Builder.Default private Boolean extendPrompt = Boolean.TRUE;

  /** The input image url, Generate the URL of the image referenced by the video */
  @Builder.Default private String imgUrl = null;

  /** The input audio url. */
  @Builder.Default private String audioUrl = null;

  /** list of character reference video file urls uploaded by the user */
  @Builder.Default private List<String> referenceVideoUrls = null;

  /** list of character reference file urls uploaded by the user */
  @Builder.Default private List<String> referenceUrls = null;

  /** list of character reference file url uploaded by the user */
  @Builder.Default private String referenceUrl = null;

  /** list of media file urls uploaded by the user */
  @Builder.Default private List<Media> media = null;

  /**
   * For the description information of the picture and sound of the reference video, corresponding
   * to ref video, it needs to be in the order of the url. If the quantity is different, an error
   * will be reported
   */
  @Builder.Default private List<String> referenceVideoDescription = null;

  /** The extra parameters. */
  @GsonExclude @Singular protected Map<String, Object> extraInputs;

  /** Duration of video generation. The default value is 5, in seconds */
  @Builder.Default private Integer duration = null;

  /** The URL of the first frame image for generating the video. */
  @Builder.Default private String firstFrameUrl = null;

  /** The URL of the last frame image for generating the video. */
  @Builder.Default private String lastFrameUrl = null;

  @Builder.Default private String headFrame = null;

  @Builder.Default private String tailFrame = null;

  @Builder.Default private Boolean withAudio = Boolean.FALSE;

  /** The resolution of the generated video */
  @Builder.Default private String resolution = null;

  @Builder.Default private Boolean promptExtend = null;

  @Builder.Default private Boolean watermark = null;

  @Builder.Default private Boolean audio = null;

  @Builder.Default private String shotType = null;

  /** The enable_overlays parameter. */
  @Builder.Default private Boolean enableOverlays = null;

  @Builder.Default private String ratio = null;

  /** The inputs of the model. */
  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    if (prompt != null && !prompt.isEmpty()) {
      jsonObject.addProperty(PROMPT, prompt);
    }

    jsonObject.addProperty(EXTEND_PROMPT, extendPrompt);

    if (negative_prompt != null && !negative_prompt.isEmpty()) {
      jsonObject.addProperty(NEGATIVE_PROMPT, negative_prompt);
    }
    if (negativePrompt != null && !negativePrompt.isEmpty()) {
      jsonObject.addProperty(NEGATIVE_PROMPT, negativePrompt);
    }
    if (template != null && !template.isEmpty()) {
      jsonObject.addProperty(TEMPLATE, template);
    }
    if (imgUrl != null && !imgUrl.isEmpty()) {
      jsonObject.addProperty(IMG_URL, imgUrl);
    }
    if (audioUrl != null && !audioUrl.isEmpty()) {
      jsonObject.addProperty(AUDIO_URL, audioUrl);
    }

    if (firstFrameUrl != null && !firstFrameUrl.isEmpty()) {
      jsonObject.addProperty(FIRST_FRAME_URL, firstFrameUrl);
    }

    if (lastFrameUrl != null && !lastFrameUrl.isEmpty()) {
      jsonObject.addProperty(LAST_FRAME_URL, lastFrameUrl);
    }

    if (headFrame != null && !headFrame.isEmpty()) {
      jsonObject.addProperty(HEAD_FRAME, headFrame);
    }

    if (tailFrame != null && !tailFrame.isEmpty()) {
      jsonObject.addProperty(TAIL_FRAME, tailFrame);
    }

    if (referenceVideoUrls != null && !referenceVideoUrls.isEmpty()) {
      jsonObject.add(REFERENCE_VIDEO_URLS, JsonUtils.toJsonArray(referenceVideoUrls));
    }

    if (referenceVideoDescription != null && !referenceVideoDescription.isEmpty()) {
      jsonObject.add(REFERENCE_VIDEO_DESCRIPTION, JsonUtils.toJsonArray(referenceVideoDescription));
    }

    if (referenceUrls != null && !referenceUrls.isEmpty()) {
      jsonObject.add(REFERENCE_URLS, JsonUtils.toJsonArray(referenceUrls));
    }

    if (referenceUrl != null && !referenceUrl.isEmpty()) {
      jsonObject.addProperty(REFERENCE_URL, referenceUrl);
    }

    if (media != null && !media.isEmpty()) {
      jsonObject.add(MEDIA_URLS, JsonUtils.toJsonArray(media));
    }

    if (extraInputs != null && !extraInputs.isEmpty()) {
      JsonObject extraInputsJsonObject = JsonUtils.parametersToJsonObject(extraInputs);
      JsonUtils.merge(jsonObject, extraInputsJsonObject);
    }
    return jsonObject;
  }

  /** The parameters of the model. */
  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (duration != null) {
      params.put(DURATION, duration);
    }

    if (size != null) {
      params.put(SIZE, size);
    }

    if (resolution != null) {
      params.put(RESOLUTION, resolution);
    }

    params.put(WITH_AUDIO, withAudio);

    if (steps != null) {
      params.put(STEPS, steps);
    }
    if (seed != null) {
      params.put(SEED, seed);
    }
    if (promptExtend != null) {
      params.put(PROMPT_EXTEND, promptExtend);
    }
    if (watermark != null) {
      params.put(WATERMARK, watermark);
    }
    if (audio != null) {
      params.put(AUDIO, audio);
    }
    if (shotType != null) {
      params.put(SHOT_TYPE, shotType);
    }
    if (enableOverlays != null) {
      params.put(ENABLE_OVERLAYS, enableOverlays);
    }
    if (ratio != null) {
      params.put(RATIO, ratio);
    }

    params.putAll(super.getParameters());
    return params;
  }

  /** The http body of the request. */
  @Override
  public JsonObject getHttpBody() {
    // this.validate();
    JsonObject body = new JsonObject();
    body.addProperty(MODEL, getModel());
    body.add(INPUT, getInput());
    Map<String, Object> params = getParameters();
    if (params != null) {
      body.add(PARAMETERS, JsonUtils.parametersToJsonObject(params));
    }
    return body;
  }

  /** The binary data of the request. */
  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  /** Validate all parameters. */
  @Override
  public void validate() throws InputRequiredException {}

  public void checkAndUpload() throws NoApiKeyException, UploadFileException {
    class UploadTaskResult {
      final String key;
      final String newUrl;
      final boolean uploaded;

      UploadTaskResult(String key, String newUrl, boolean uploaded) {
        this.key = key;
        this.newUrl = newUrl;
        this.uploaded = uploaded;
      }
    }

    List<CompletableFuture<UploadTaskResult>> futures = new ArrayList<>();

    class TaskItem {
      final String keyPrefix;
      final String keyItemPrefix;
      final String value;
      final int index;

      TaskItem(String keyPrefix, String keyItemPrefix, String value, int index) {
        this.keyPrefix = keyPrefix;
        this.keyItemPrefix = keyItemPrefix;
        this.value = value;
        this.index = index;
      }

      TaskItem(String keyPrefix, String value, int index) {
        this.keyPrefix = keyPrefix;
        this.keyItemPrefix = "";
        this.value = value;
        this.index = index;
      }

      TaskItem(String keyPrefix, String value) {
        this.keyPrefix = keyPrefix;
        this.keyItemPrefix = "";
        this.value = value;
        this.index = -1;
      }

      String getFullKey() {
        return index >= 0 ? keyPrefix + keyItemPrefix + "[" + index + "]" : keyPrefix;
      }
    }

    List<TaskItem> itemsToProcess = new ArrayList<>();

    if (this.imgUrl != null) itemsToProcess.add(new TaskItem(IMG_URL, this.imgUrl));
    if (this.audioUrl != null) itemsToProcess.add(new TaskItem(AUDIO_URL, this.audioUrl));
    if (this.firstFrameUrl != null)
      itemsToProcess.add(new TaskItem(FIRST_FRAME_URL, this.firstFrameUrl));
    if (this.lastFrameUrl != null)
      itemsToProcess.add(new TaskItem(LAST_FRAME_URL, this.lastFrameUrl));
    if (this.headFrame != null) itemsToProcess.add(new TaskItem(HEAD_FRAME, this.headFrame));
    if (this.tailFrame != null) itemsToProcess.add(new TaskItem(TAIL_FRAME, this.tailFrame));
    if (this.referenceUrl != null)
      itemsToProcess.add(new TaskItem(REFERENCE_URL, this.referenceUrl));

    if (this.referenceVideoUrls != null) {
      for (int i = 0; i < this.referenceVideoUrls.size(); i++) {
        String url = this.referenceVideoUrls.get(i);
        if (url != null) {
          itemsToProcess.add(new TaskItem(REFERENCE_VIDEO_URLS, url, i));
        }
      }
    }

    if (this.referenceUrls != null) {
      for (int i = 0; i < this.referenceUrls.size(); i++) {
        String url = this.referenceUrls.get(i);
        if (url != null) {
          itemsToProcess.add(new TaskItem(REFERENCE_URLS, url, i));
        }
      }
    }

    if (this.media != null) {
      for (int i = 0; i < this.media.size(); i++) {
        Media media = this.media.get(i);
        if (media != null) {
          if (media.getUrl() != null) {
            itemsToProcess.add(new TaskItem(MEDIA_URLS, "_URL", media.getUrl(), i));
          }
          if (media.getReferenceVoice() != null) {
            itemsToProcess.add(
                new TaskItem(MEDIA_URLS, "_REFERENCE_VOICE", media.getReferenceVoice(), i));
          }
        }
      }
    }

    if (itemsToProcess.isEmpty()) {
      return;
    }

    ExecutorService executor = Executors.newFixedThreadPool(5);
    try {
      for (TaskItem item : itemsToProcess) {
        CompletableFuture<UploadTaskResult> future =
            CompletableFuture.supplyAsync(
                () -> {
                  Map<String, String> singleCheckMap = new HashMap<>();
                  String fullKey = item.getFullKey();
                  singleCheckMap.put(fullKey, item.value);

                  boolean isUploaded;
                  try {
                    isUploaded =
                        PreprocessInputImage.checkAndUploadImage(
                            getModel(), singleCheckMap, getApiKey());
                  } catch (NoApiKeyException | UploadFileException e) {
                    throw new RuntimeException(e);
                  }

                  return new UploadTaskResult(fullKey, singleCheckMap.get(fullKey), isUploaded);
                },
                executor);
        futures.add(future);
      }
    } finally {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    List<UploadTaskResult> results = new ArrayList<>();
    boolean globalIsUpload = false;

    try {
      for (CompletableFuture<UploadTaskResult> future : futures) {
        UploadTaskResult result = future.get();
        results.add(result);
        if (result.uploaded) {
          globalIsUpload = true;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Upload process interrupted", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof NoApiKeyException) {
        throw (NoApiKeyException) cause;
      } else if (cause instanceof UploadFileException) {
        throw (UploadFileException) cause;
      } else {
        throw new RuntimeException("Upload failed", cause);
      }
    }

    if (globalIsUpload) {
      this.putHeader("X-DashScope-OssResourceResolve", "enable");

      Map<String, String> resultMap =
          results.stream().collect(Collectors.toMap(r -> r.key, r -> r.newUrl));

      if (resultMap.containsKey(IMG_URL)) this.imgUrl = resultMap.get(IMG_URL);
      if (resultMap.containsKey(AUDIO_URL)) this.audioUrl = resultMap.get(AUDIO_URL);
      if (resultMap.containsKey(FIRST_FRAME_URL))
        this.firstFrameUrl = resultMap.get(FIRST_FRAME_URL);
      if (resultMap.containsKey(LAST_FRAME_URL)) this.lastFrameUrl = resultMap.get(LAST_FRAME_URL);
      if (resultMap.containsKey(HEAD_FRAME)) this.headFrame = resultMap.get(HEAD_FRAME);
      if (resultMap.containsKey(TAIL_FRAME)) this.tailFrame = resultMap.get(TAIL_FRAME);
      if (resultMap.containsKey(REFERENCE_URL)) this.referenceUrl = resultMap.get(REFERENCE_URL);

      if (this.referenceVideoUrls != null && !this.referenceVideoUrls.isEmpty()) {
        List<String> newVideos = new ArrayList<>(this.referenceVideoUrls.size());
        for (int i = 0; i < this.referenceVideoUrls.size(); i++) {
          String key = REFERENCE_VIDEO_URLS + "[" + i + "]";
          newVideos.add(resultMap.getOrDefault(key, this.referenceVideoUrls.get(i)));
        }
        this.referenceVideoUrls = newVideos;
      }

      if (this.referenceUrls != null && !this.referenceUrls.isEmpty()) {
        List<String> newRefs = new ArrayList<>(this.referenceUrls.size());
        for (int i = 0; i < this.referenceUrls.size(); i++) {
          String key = REFERENCE_URLS + "[" + i + "]";
          newRefs.add(resultMap.getOrDefault(key, this.referenceUrls.get(i)));
        }
        this.referenceUrls = newRefs;
      }

      if (this.media != null && !this.media.isEmpty()) {
        for (int i = 0; i < this.media.size(); i++) {
          Media mediaItem = this.media.get(i);

          String urlKey = MEDIA_URLS + "_URL[" + i + "]";
          String voiceKey = MEDIA_URLS + "_REFERENCE_VOICE[" + i + "]";
          if (resultMap.containsKey(urlKey)) {
            mediaItem.setUrl(resultMap.get(urlKey));
          }
          if (resultMap.containsKey(voiceKey)) {
            mediaItem.setReferenceVoice(resultMap.get(voiceKey));
          }
        }
      }
    }
  }
}
