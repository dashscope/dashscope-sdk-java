package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.common.ResultCallback;
import io.reactivex.Flowable;
import java.io.File;
import java.nio.ByteBuffer;

public interface BaseRecognition {
  /**
   * Call the server to do a stream-in, stream-out realtime recognition. RecognitionParam's
   * audioFrames must be set with a Flowable of audio frames.
   *
   * @param param The input param, should be the subclass of `RecognitionParam`.
   * @return A `Flowable` of the output structure..
   */
  Flowable<RecognitionResult> streamCall(RecognitionParam param, Flowable<ByteBuffer> audioFrames);

  /**
   * Call the server to do a callback style realtime recognition.
   *
   * @param param The input param, should be the subclass of `RecognitionParam`.
   * @param callback The callback to receive response, should be the subclass of
   *     `RecognitionResult`.
   */
  void call(RecognitionParam param, ResultCallback<RecognitionResult> callback);

  /**
   * Call the server to do a blocking style recognition from file.
   *
   * @param param The input param, should be the subclass of `RecognitionParam`.
   * @param file The file to be recognized.
   * @return The reognition result.
   */
  String call(RecognitionParam param, File file);

  /**
   * Send audio frame to server. Must be called after `call`. Do not mix with `streamCall`.
   *
   * @param audioFrame The audio frame to send.
   */
  void sendAudioFrame(ByteBuffer audioFrame);

  /** Stop the recognition. Must be called after `call`. Do not mix with `streamCall`. */
  void stop();
}
