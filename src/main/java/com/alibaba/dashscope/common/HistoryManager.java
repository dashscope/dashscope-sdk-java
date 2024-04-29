// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.google.common.collect.EvictingQueue;
import java.util.Arrays;
import java.util.List;

/** will be delete next release. */
@Deprecated
public final class HistoryManager {
  private static final int DEFAULT_MAXIMUM_MESSAGES = 10;
  private final EvictingQueue<History> history;

  public HistoryManager() {
    history = EvictingQueue.create(DEFAULT_MAXIMUM_MESSAGES);
  }

  public HistoryManager(int maxMessages) {
    history = EvictingQueue.create(maxMessages);
  }

  public void add(History msg) {
    history.add(msg);
  }

  /**
   * Get all the `Message`
   *
   * @return The `Message` list.
   */
  public List<History> get() {
    return Arrays.asList(history.toArray(new History[0]));
  }

  /**
   * Get part of `Message`
   *
   * @param start The start index.
   * @param end The end index
   * @return The `Message` list.
   */
  public List<History> get(int start, int end) {
    return Arrays.asList(history.toArray(new History[0])).subList(start, end);
  }
}
