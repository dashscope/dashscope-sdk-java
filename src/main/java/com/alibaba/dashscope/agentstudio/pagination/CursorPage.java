// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.pagination;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CursorPage<T> extends FlattenResultBase implements Iterable<T> {
  @SerializedName("data")
  private List<T> data;

  @SerializedName("next_page")
  private String nextPage;

  @EqualsAndHashCode.Exclude
  private transient Function<String, CompletableFuture<CursorPage<T>>> fetchNext;

  public boolean hasNext() {
    return nextPage != null && fetchNext != null;
  }

  public CompletableFuture<CursorPage<T>> getNext() {
    if (!hasNext()) {
      return CompletableFuture.completedFuture(null);
    }
    return fetchNext.apply(nextPage);
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private CursorPage<T> currentPage = CursorPage.this;
      private int index = 0;

      @Override
      public boolean hasNext() {
        if (currentPage == null) {
          return false;
        }
        while (true) {
          if (currentPage.data != null && index < currentPage.data.size()) {
            return true;
          }
          if (!currentPage.hasNext()) {
            currentPage = null;
            return false;
          }
          currentPage = currentPage.getNext().join();
          if (currentPage == null) {
            return false;
          }
          index = 0;
        }
      }

      @Override
      public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        return currentPage.data.get(index++);
      }
    };
  }
}
