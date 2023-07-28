package com.alibaba.dashscope.aigc.imagesynthesis;

import com.alibaba.dashscope.common.AsyncTaskInfo;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ImageSynthesisListResult {
  @SerializedName("request_id")
  private String requestId;

  private List<AsyncTaskInfo> data;
  private Integer total;

  @SerializedName("total_page")
  private Integer totalPage;

  @SerializedName("page_no")
  private Integer pageNo;

  @SerializedName("page_size")
  private Integer pageSize;

  public static ImageSynthesisListResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    if (dashScopeResult.getOutput() != null) {
      ImageSynthesisListResult rs =
          (JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), ImageSynthesisListResult.class));
      rs.requestId = dashScopeResult.getRequestId();
      return rs;
    } else {
      log.error(String.format("Result no output: %s", dashScopeResult));
    }
    return null;
  }
}
