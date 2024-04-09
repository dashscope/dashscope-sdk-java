package com.alibaba.dashscope.tokenizers;

import java.util.Arrays;

class EncodeBytesEntity {
  public final byte[] bytes;
  public int rank = Integer.MAX_VALUE;

  public EncodeBytesEntity(byte[] bytes) {
    this.bytes = bytes;
  }

  public EncodeBytesEntity(byte[] bytes, int rank) {
    this.bytes = bytes;
    this.rank = rank;
  }

  int length() {
    return bytes.length;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (other == null || getClass() != other.getClass()) {
      return false;
    }
    return Arrays.equals(bytes, ((EncodeBytesEntity) other).bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    return Arrays.toString(bytes);
  }
}
