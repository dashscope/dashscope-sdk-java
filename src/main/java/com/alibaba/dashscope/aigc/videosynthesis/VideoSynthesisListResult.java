package com.alibaba.dashscope.aigc.videosynthesis;

import com.alibaba.dashscope.common.AsyncTaskInfo;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class VideoSynthesisListResult {
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

  public static VideoSynthesisListResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    if (dashScopeResult.getOutput() != null) {
      VideoSynthesisListResult rs =
          (JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), VideoSynthesisListResult.class));
      rs.requestId = dashScopeResult.getRequestId();
      return rs;
    } else {
      log.error("Result no output: {}", dashScopeResult);
    }
    return null;
  }
}
