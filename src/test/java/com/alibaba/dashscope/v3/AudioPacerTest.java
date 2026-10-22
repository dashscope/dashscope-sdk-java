// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.resources.audio.AudioPacer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** AudioPacer chunking/pacing semantics (audio design §4). */
class AudioPacerTest {

  @TempDir Path dir;

  @Test
  void bytesPer100msFollowsSampleRate() {
    assertEquals(3200, AudioPacer.bytesPer100ms(16000));
    assertEquals(4800, AudioPacer.bytesPer100ms(24000));
    assertEquals(9600, AudioPacer.bytesPer100ms(24000, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> AudioPacer.bytesPer100ms(0));
    assertThrows(IllegalArgumentException.class, () -> AudioPacer.bytesPer100ms(16000, 0, 1));
  }

  @Test
  void inMemoryChunksKeepOrderAndTail() {
    byte[] pcm = new byte[10];
    for (int i = 0; i < pcm.length; i++) {
      pcm[i] = (byte) i;
    }
    List<byte[]> chunks = collect(AudioPacer.pcmChunks(pcm, 4));
    assertEquals(3, chunks.size());
    assertArrayEquals(new byte[] {0, 1, 2, 3}, chunks.get(0));
    assertArrayEquals(new byte[] {4, 5, 6, 7}, chunks.get(1));
    assertArrayEquals(new byte[] {8, 9}, chunks.get(2));
  }

  @Test
  void fileChunksAreLazyAndCloseCleanly() throws IOException {
    Path file = dir.resolve("a.pcm");
    byte[] pcm = new byte[7000];
    for (int i = 0; i < pcm.length; i++) {
      pcm[i] = (byte) (i % 128);
    }
    Files.write(file, pcm);

    List<byte[]> chunks = collect(AudioPacer.pcmChunks(file, AudioPacer.bytesPer100ms(16000)));
    // 7000 bytes / 3200 per chunk = 2 full chunks + a 600-byte tail.
    assertEquals(3, chunks.size());
    assertEquals(3200, chunks.get(0).length);
    assertEquals(3200, chunks.get(1).length);
    assertEquals(600, chunks.get(2).length);
    assertEquals(pcm[6400], chunks.get(2)[0]);
    // File was closed by the iterator: deletion must succeed.
    Files.delete(file);
  }

  @Test
  void emptyFileYieldsNoChunks() throws IOException {
    Path file = dir.resolve("empty.pcm");
    Files.write(file, new byte[0]);
    assertEquals(0, collect(AudioPacer.pcmChunks(file, 100)).size());
  }

  @Test
  void missingFileRaisesConnectionException() {
    ApiConnectionException e =
        assertThrows(
            ApiConnectionException.class,
            () -> collect(AudioPacer.pcmChunks(dir.resolve("nope.pcm"), 100)));
    assertTrue(e.getMessage().contains("nope.pcm"));
  }

  @Test
  void paceCatchesUpInstantlyWhenBehind() {
    long start = System.nanoTime();
    long t0 = System.nanoTime();
    AudioPacer.pace(start, 0);
    assertTrue(System.nanoTime() - t0 < 50_000_000L, "pace(0) should return immediately");
  }

  @Test
  void paceWaitsForChunkDeadline() {
    long start = System.nanoTime();
    AudioPacer.pace(start, 0);
    AudioPacer.pace(start, 1);
    long elapsed = System.nanoTime() - start;
    assertTrue(
        elapsed >= AudioPacer.CHUNK_INTERVAL.toNanos() - 20_000_000L,
        "pace(1) returned too early: " + elapsed);
  }

  @Test
  void paceInterruptionRaisesAndRestoresFlag() {
    Thread.currentThread().interrupt();
    long start = System.nanoTime();
    try {
      assertThrows(ApiTimeoutException.class, () -> AudioPacer.pace(start, 1));
      assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag must be restored");
    } finally {
      Thread.interrupted();
    }
  }

  private static List<byte[]> collect(Iterable<byte[]> chunks) {
    List<byte[]> out = new ArrayList<>();
    for (byte[] c : chunks) {
      out.add(c);
    }
    return out;
  }
}
