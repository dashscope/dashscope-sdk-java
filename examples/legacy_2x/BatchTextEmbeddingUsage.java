import com.alibaba.dashscope.embeddings.BatchTextEmbedding;
import com.alibaba.dashscope.embeddings.BatchTextEmbeddingParam;
import com.alibaba.dashscope.embeddings.BatchTextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.alibaba.dashscope.task.AsyncTaskListResult;
import com.alibaba.dashscope.utils.JsonUtils;

public class BatchTextEmbeddingUsage {
  public static void basicCall() throws ApiException, NoApiKeyException {
    BatchTextEmbeddingParam param =
        BatchTextEmbeddingParam.builder()
            .model(BatchTextEmbedding.Models.TEXT_EMBEDDING_ASYNC_V1)
            .url("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt")
            .build();
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    BatchTextEmbeddingResult result = textEmbedding.call(param);
    System.out.println(result);
  }

  // Create a batch task
  public static BatchTextEmbeddingResult createTask() throws ApiException, NoApiKeyException {
    BatchTextEmbeddingParam param =
        BatchTextEmbeddingParam.builder()
            .model(BatchTextEmbedding.Models.TEXT_EMBEDDING_ASYNC_V1)
            .url("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt")
            .build();
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    return textEmbedding.asyncCall(param);
  }

  // Get the task status
  public static void fetchTaskStatus(BatchTextEmbeddingResult result)
      throws ApiException, NoApiKeyException {
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    result = textEmbedding.fetch(result, null);
    System.out.println(result);
  }

  // Wait for the task to finish; wait polls until completion
  public static void waitTask(BatchTextEmbeddingResult result)
      throws ApiException, NoApiKeyException {
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    result = textEmbedding.wait(result, null);
    System.out.println(result);
  }

  // Cancel a task; only pending tasks can be canceled
  public static void cancelTask(BatchTextEmbeddingResult result)
      throws ApiException, NoApiKeyException {
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    result = textEmbedding.cancel(result, null);
    System.out.println(result);
  }

  // List submitted tasks
  public static void list() throws ApiException, NoApiKeyException {
    AsyncTaskListParam param = AsyncTaskListParam.builder().pageNo(1).pageSize(20).build();
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    AsyncTaskListResult result = textEmbedding.list(param);
    System.out.println(JsonUtils.toJson(result));
  }

  public static void main(String[] args) {
    try {
      BatchTextEmbeddingResult task = createTask();
      fetchTaskStatus(task);
      waitTask(task);
      // list();
    } catch (ApiException | NoApiKeyException e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
