package com.alibaba.dashscope.tools.search;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * used in step eg: { "assistant_id": "asst_2f700e30-2062-42df-8010-3c34ce5a9417", "cancelled_at":
 * -1, "completed_at": -1, "created_at": 1712909007434, "expires_at": null, "failed_at": -1, "id":
 * "step_eea4635c-07d4-4708-bd12-0804921a3b93", "last_error": { "code": "", "message": "" },
 * "metadata": {}, "object": "thread.run.step", "run_id":
 * "run_06c28eae-9afd-4e33-ba34-c2e8f783861e", "started_at": null, "status": "completed",
 * "step_details": { "tool_calls": [ { "id": "", "quark_search": { "input": "{\"query\":
 * \"北京今天天气\"}", "output": "{\"success\": true, \"errorCode\": null, \"errorMsg\": null, \"data\":
 * [json_string of result]}"; }, "type": "quark_search" } ], "type": "tool_calls" }, "thread_id":
 * "thread_70a41285-1894-4f8d-97ac-9dc257cd3d3d", "type": "tool_calls", "usage": {} }
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ToolCallQuarkSearch extends ToolCallBase {
  private String type = "quark_search";
  private String id;

  @SerializedName("quark_search")
  private Map<String, String> quarkSearch;

  static {
    registerToolCall("quark_search", ToolCallQuarkSearch.class);
  }
}
