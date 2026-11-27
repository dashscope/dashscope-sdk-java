// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Local audio helpers for the examples (translation of the Python blueprint's {@code _common.py}):
 * WAV container read/write, microphone capture and playback. Pure JDK ({@code javax.sound}), no
 * cloud calls.
 */
public final class AudioCommon {

  /** Decoded 16-bit mono PCM plus its sample rate. */
  public static final class PcmAudio {
    public final byte[] pcm;
    public final int sampleRate;

    PcmAudio(byte[] pcm, int sampleRate) {
      this.pcm = pcm;
      this.sampleRate = sampleRate;
    }
  }

  private AudioCommon() {}

  /**
   * Read a local WAV as raw PCM. Requires mono, 16-bit PCM at one of {@code allowedRates}; no
   * resampling is performed (convert with ffmpeg first if needed).
   */
  public static PcmAudio readWav(Path path, int... allowedRates) throws Exception {
    AudioFormat format;
    byte[] pcm;
    try (AudioInputStream in = AudioSystem.getAudioInputStream(path.toFile())) {
      format = in.getFormat();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) >= 0) {
        out.write(buf, 0, n);
      }
      pcm = out.toByteArray();
    }
    if (format.getChannels() != 1
        || format.getSampleSizeInBits() != 16
        || format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
      throw new IllegalArgumentException(
          "need mono 16-bit PCM WAV; actual: " + format + ". Convert with ffmpeg first.");
    }
    int rate = (int) format.getSampleRate();
    for (int allowed : allowedRates) {
      if (rate == allowed) {
        return new PcmAudio(pcm, rate);
      }
    }
    throw new IllegalArgumentException(
        "need sample rate in " + java.util.Arrays.toString(allowedRates) + "; actual: " + rate);
  }

  /** Write raw 16-bit mono PCM into a new WAV file; never overwrites an existing file. */
  public static void writeWav(Path path, byte[] pcm, int sampleRate) throws IOException {
    if (!path.toString().toLowerCase().endsWith(".wav")) {
      throw new IllegalArgumentException("PCM output must use the .wav extension");
    }
    if (Files.exists(path)) {
      throw new IllegalArgumentException("output already exists: " + path);
    }
    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
    try (AudioInputStream in =
        new AudioInputStream(new ByteArrayInputStream(pcm), format, pcm.length / 2)) {
      AudioSystem.write(in, AudioFileFormat.Type.WAVE, path.toFile());
    }
  }

  /** Play a WAV synchronously; when no audio line exists (CI/headless), print a hint and skip. */
  public static void play(Path path) {
    try (AudioInputStream in = AudioSystem.getAudioInputStream(path.toFile())) {
      Clip clip = AudioSystem.getClip();
      clip.open(in);
      clip.start();
      while (clip.isRunning()) {
        Thread.sleep(50);
      }
      clip.close();
    } catch (LineUnavailableException | IOException e) {
      System.err.println("playback unavailable (" + e.getMessage() + "); file kept: " + path);
    } catch (Exception e) {
      throw new RuntimeException("playback failed: " + path, e);
    }
  }

  /** Open the default microphone at the given rate as 16-bit mono PCM. */
  public static TargetDataLine microphone(int sampleRate) throws LineUnavailableException {
    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
    TargetDataLine line = AudioSystem.getTargetDataLine(format);
    line.open(format, sampleRate / 10 * 2);
    line.start();
    return line;
  }
}
