package com.alibaba.dashscope.audio.tts;

import com.alibaba.dashscope.common.ResultCallback;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;

public interface BaseSpeechSynthesizer {

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param, should be the subclass of `SynthesisParam`.
   * @param callback The callback to receive response, should be the subclass of `SynthesisResult`.
   */
  void call(SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback);

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `SynthesisParam`.
   * @return A `Flowable` of the output structure, which is the subclass of `SynthesisResult`.
   */
  Flowable<SpeechSynthesisResult> streamCall(SpeechSynthesisParam param);

  /**
   * Call the server to get the result. This method will block the thread until the result is
   * received. Get audio data and timestamps by calling SynthesisResult's getAudio() and
   * getTimestamps().
   *
   * @param param The input param, should be the subclass of `SynthesisParam`.
   */
  ByteBuffer call(SpeechSynthesisParam param);
}
