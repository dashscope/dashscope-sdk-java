import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.multimodal.tingwu.TingWu;
import com.alibaba.dashscope.multimodal.tingwu.TingWuParam;
import com.alibaba.dashscope.utils.JsonUtils;
import java.util.HashMap;
import java.util.Map;

/** */
public class TingWuUsage {

  public static Map<String, Object> buildCreateTask(String appId) {
    Map<String, Object> params = new HashMap<>();
    params.put("fileUrl", "http://demo.com/test.mp3");
    params.put("appid", appId);
    params.put("task", "createTask");
    // Set additional parameters
    return params;
  }

  public static void main(String[] args) {
    try {
      // Create the task
      TingWu tingwu = new TingWu();
      TingWuParam param =
          TingWuParam.builder()
              .model("tingwu-automotive-service-inspection")
              .input(buildCreateTask("123456"))
              .parameters(new HashMap<>()) // Set task parameters
              .build();
      DashScopeResult result = tingwu.call(param);
      System.out.println(JsonUtils.toJson(result));
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
