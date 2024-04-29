package com.alibaba.dashscope.common;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GeneralListParam extends GeneralGetParam {
  private Long limit;
  private String order;
  private String before;
  private String after;

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (limit != null) {
      params.put("limit", limit);
    }
    if (order != null && !order.isEmpty()) {
      params.put("order", order);
    }
    if (before != null && !before.isEmpty()) {
      params.put("before", before);
    }
    if (after != null && !after.isEmpty()) {
      params.put("after", after);
    }
    params.putAll(this.parameters);
    return params;
  }
}
