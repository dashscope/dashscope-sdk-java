// Copyright (c) Alibaba, Inc. and its affiliates.

import java.io.File;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public final class AudioRecognitionUsage {
  private final AudioFormat format = buildAudioFormatInstance();
  private volatile boolean stopped;

  public TargetDataLine getTargetDataLineForRecord() throws LineUnavailableException {
    TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
    System.out.println(microphone.getBufferSize());
    microphone.open();
    return microphone;
  }

  public Flowable<ByteBuffer> getMicrophoneStreaming() {
    Flowable<ByteBuffer> audios =
        Flowable.<ByteBuffer>create(
            emitter -> {
              try {
                final TargetDataLine line = getTargetDataLineForRecord();
                int frameSizeInBytes = format.getFrameSize();
                System.out.println(
                    String.format(
                        "Frame size in bytes: %s, %s", frameSizeInBytes, format.getEncoding()));
                final int bufferLengthInBytes = 1024 * frameSizeInBytes;
                int numBytesRead;
                line.start();
                int c = 0;
                byte[] data = new byte[bufferLengthInBytes];
                while (!stopped && c < 100) {
                  if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
                    break;
                  } else {
                    System.out.println(String.format("Read microphone %s data", numBytesRead));
                    emitter.onNext(ByteBuffer.wrap(data, 0, numBytesRead));
                  }
                  if (c % 10 == 0) {
                    System.out.println(String.format("Sending %d packages", c));
                  }
                  ++c;
                }
                emitter.onComplete();
              } catch (Exception ex) {
                ex.printStackTrace();
                emitter.onError(ex);
              }
            },
            BackpressureStrategy.BUFFER);
    return audios;
  }

  public AudioFormat buildAudioFormatInstance() {
    final AudioFormat.Encoding ENCODING = AudioFormat.Encoding.PCM_SIGNED;
    final float RATE = 16000.0f;
    final int CHANNELS = 1;
    final int SAMPLE_SIZE = 16;
    final boolean BIG_ENDIAN = false;
    return new AudioFormat(ENCODING, RATE, SAMPLE_SIZE, CHANNELS, 2, RATE, BIG_ENDIAN);
  }

  public Flowable<ByteBuffer> getStreamingDataFromFile(String filePath) {
    Flowable<ByteBuffer> audios =
        Flowable.<ByteBuffer>create(
                emmitter -> {
                  int totalFramesRead = 0;
                  File fileIn = new File(filePath);
                  try {
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fileIn);
                    int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
                    System.out.println(String.format("BytesPerFrame: %s", bytesPerFrame));
                    // Set an arbitrary buffer size of 1024 frames.
                    int numBytes = 1024 * bytesPerFrame;
                    byte[] audioBytes = new byte[numBytes];
                    try {
                      int numBytesRead = 0;
                      int numFramesRead = 0;
                      // Try to read numBytes bytes from the file.
                      while ((numBytesRead = audioInputStream.read(audioBytes)) != -1) {
                        // Calculate the number of frames actually read.
                        numFramesRead = numBytesRead / bytesPerFrame;
                        totalFramesRead += numFramesRead;
                        emmitter.onNext(ByteBuffer.wrap(audioBytes, 0, numBytesRead));
                      }
                      emmitter.onComplete();
                      System.out.println(String.format("Total frames: %d", totalFramesRead));
                    } catch (Exception ex) {
                      emmitter.onError(ex);
                    }
                  } catch (Exception e) {
                    emmitter.onError(e);
                  }
                },
                BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.io());
    return audios;
  }

  public void recognitionFile(String filePath) throws ApiException, NoApiKeyException {
    RecognitionParam param =
        RecognitionParam.builder()
            .format("pcm")
            .model("paraformer-realtime-v1")
            .sampleRate(16000)
            .build();
    Recognition rg = new Recognition();
    Flowable<RecognitionResult> resultFlowable = rg.streamCall(param, getStreamingDataFromFile(filePath));
    resultFlowable.blockingForEach(
        message -> {
          System.out.println(message);
        });
  }

  public void recognitionRealtimeMicrophone() throws ApiException, NoApiKeyException {
    RecognitionParam param =
        RecognitionParam.builder()
            .format("pcm")
            .model("paraformer-realtime-v1")
            .sampleRate(16000)
            .build();
    Recognition rg = new Recognition();
    Flowable<RecognitionResult> resultFlowable = rg.streamCall(param, getMicrophoneStreaming());
    resultFlowable
        .doOnError(
            err -> {
              stopped = true;
              System.out.println(err);
            })
        .blockingForEach(
            message -> {
              System.out.println(message);
            });
  }

  public static void main(String[] args){
    String filePath = "./src/test/resources/asr_example_cn_en.wav";
    AudioRecognitionUsage audioRecognition = new AudioRecognitionUsage();
    try {
      audioRecognition.recognitionFile(filePath);
      audioRecognition.recognitionRealtimeMicrophone();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
