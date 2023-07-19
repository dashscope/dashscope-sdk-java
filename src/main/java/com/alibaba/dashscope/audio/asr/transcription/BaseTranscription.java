package com.alibaba.dashscope.audio.asr.transcription;

public interface BaseTranscription {

  /**
   * Submit a transcription task to server and get a status result immediately. Using
   * TranscriptionResult's task_id to query task status later.
   *
   * @param param The input param, should be the subclass of `TranscriptionParam`.
   * @return The output structure, should be the subclass of `TranscriptionResult`.
   */
  TranscriptionResult asyncCall(TranscriptionParam param);

  /**
   * Waiting for the transcription task to complete.
   *
   * @param param The query param can be built from TranscriptionParam.
   * @return The output structure, should be the subclass of `TranscriptionResult`.
   */
  TranscriptionResult wait(TranscriptionQueryParam param);

  /**
   * Fetch the transcription task status.
   *
   * @param param The query param can be built from TranscriptionParam.
   * @return The output structure, should be the subclass of `TranscriptionResult`.
   */
  TranscriptionResult fetch(TranscriptionQueryParam param);
}
