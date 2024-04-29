package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AssistantDeleted */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ListResult<T> extends FlattenResultBase {
  @SerializedName("first_id")
  private String firstId;

  @SerializedName("last_id")
  private String lastId;

  @SerializedName("has_more")
  private Boolean hasMore;

  private List<T> data;
  /**
   * Object
   *
   * <p>(Required)
   */
  @SerializedName("object")
  private String object = "list";
}
