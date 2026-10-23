// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import com.alibaba.dashscope.v3.errors.ApiConnectionException;
import com.alibaba.dashscope.v3.errors.ApiTimeoutException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.LockSupport;

/**
 * PCM chunking + pacing helpers (audio design §4/§12): chunk size is derived from the sample rate —
 * one chunk carries exactly {@value #CHUNK_MILLIS} ms of audio, so the classic "sleep 100 ms per
 * chunk" upload loop stays real-time at any format. This fixes the 2.x pattern of a fixed 16 KB
 * chunk + sleep(100) that drifted off real time for non-16kHz/mono/16-bit sources.
 */
public final class AudioPacer {
  private static final long CHUNK_MILLIS = 100;
  /** Wall-clock interval between consecutive chunks in a paced upload loop. */
  public static final Duration CHUNK_INTERVAL = Duration.ofMillis(CHUNK_MILLIS);

  private AudioPacer() {}

  /** Bytes per 100 ms of pcm16 mono audio at {@code sampleRate} (e.g. 3200 for 16000 Hz). */
  public static int bytesPer100ms(int sampleRate) {
    return bytesPer100ms(sampleRate, 2, 1);
  }

  /** Bytes per 100 ms for arbitrary PCM layouts. */
  public static int bytesPer100ms(int sampleRate, int bytesPerSample, int channels) {
    if (sampleRate <= 0 || bytesPerSample <= 0 || channels <= 0) {
      throw new IllegalArgumentException("sampleRate/bytesPerSample/channels must be positive");
    }
    return sampleRate * bytesPerSample * channels / 10;
  }

  /**
   * Lazily split a PCM file into {@code bytesPerChunk} byte chunks (the final short chunk is
   * included; an empty file yields no chunks). The file is opened when iteration starts and closed
   * when it ends.
   */
  public static Iterable<byte[]> pcmChunks(Path path, int bytesPerChunk) {
    if (bytesPerChunk <= 0) {
      throw new IllegalArgumentException("bytesPerChunk must be positive");
    }
    return () ->
        new Iterator<byte[]>() {
          private final InputStream in = open(path);
          private byte[] pending;
          private boolean loaded;
          private boolean finished;

          @Override
          public boolean hasNext() {
            if (!loaded && !finished) {
              pending = readChunk();
              loaded = true;
            }
            return !finished;
          }

          @Override
          public byte[] next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            byte[] out = pending;
            pending = null;
            loaded = false;
            return out;
          }

          private byte[] readChunk() {
            byte[] buf = new byte[bytesPerChunk];
            int off = 0;
            try {
              while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) {
                  break;
                }
                off += n;
              }
            } catch (IOException e) {
              closeQuietly();
              throw new ApiConnectionException("failed to read audio file " + path, e);
            }
            if (off == 0) {
              finished = true;
              closeQuietly();
              return null;
            }
            if (off == buf.length) {
              return buf;
            }
            byte[] tail = new byte[off];
            System.arraycopy(buf, 0, tail, 0, off);
            return tail;
          }

          private void closeQuietly() {
            try {
              in.close();
            } catch (IOException e) {
              throw new ApiConnectionException("failed to close audio file " + path, e);
            }
          }
        };
  }

  /** Split an in-memory PCM buffer (final short chunk included). */
  public static Iterable<byte[]> pcmChunks(byte[] pcm, int bytesPerChunk) {
    if (bytesPerChunk <= 0) {
      throw new IllegalArgumentException("bytesPerChunk must be positive");
    }
    List<byte[]> chunks = new ArrayList<>();
    for (int off = 0; off < pcm.length; off += bytesPerChunk) {
      int len = Math.min(bytesPerChunk, pcm.length - off);
      byte[] chunk = new byte[len];
      System.arraycopy(pcm, off, chunk, 0, len);
      chunks.add(chunk);
    }
    return chunks;
  }

  /**
   * Drift-free pacing: block until {@code startNanos + chunkIndex * CHUNK_INTERVAL} (nanoTime
   * clock). Call once per sent chunk with a fixed {@code startNanos}; catch-up is immediate when
   * the caller falls behind, and no drift accumulates when it keeps up.
   */
  public static void pace(long startNanos, int chunkIndex) {
    long target = startNanos + chunkIndex * CHUNK_INTERVAL.toNanos();
    long remaining;
    while ((remaining = target - System.nanoTime()) > 0) {
      LockSupport.parkNanos(remaining);
      if (Thread.interrupted()) {
        Thread.currentThread().interrupt();
        throw new ApiTimeoutException("interrupted while pacing audio chunks", null);
      }
    }
  }

  private static InputStream open(Path path) {
    try {
      return Files.newInputStream(path);
    } catch (IOException e) {
      throw new ApiConnectionException("failed to open audio file " + path, e);
    }
  }
}
