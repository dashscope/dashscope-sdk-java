package com.alibaba.dashscope.task;

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
public class AsyncTaskListResult {
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

  @SerializedName("status_code")
  private Integer statusCode;

  private String code;

  private String message;

  public static AsyncTaskListResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    if (dashScopeResult.getOutput() != null) {
      AsyncTaskListResult rs =
          (JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), AsyncTaskListResult.class));
      rs.requestId = dashScopeResult.getRequestId();
      rs.statusCode = dashScopeResult.getStatusCode();
      rs.code = dashScopeResult.getCode();
      rs.message = dashScopeResult.getMessage();
      return rs;
    } else {
      log.error(String.format("Result no output: %s", dashScopeResult));
    }
    return null;
  }
}
