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
        BatchTextEmbeddingParam param = BatchTextEmbeddingParam.builder()
                .model(BatchTextEmbedding.Models.TEXT_EMBEDDING_ASYNC_V1)
                .url("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt")
                .build();
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        BatchTextEmbeddingResult result = textEmbedding.call(param);
        System.out.println(result);
    }

    // 创建批处理任务，
    public static BatchTextEmbeddingResult createTask() throws ApiException, NoApiKeyException {
        BatchTextEmbeddingParam param = BatchTextEmbeddingParam.builder()
                .model(BatchTextEmbedding.Models.TEXT_EMBEDDING_ASYNC_V1)
                .url("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt")
                .build();
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        return textEmbedding.asyncCall(param);

    }

    // 获取任务状态
    public static void fetchTaskStatus(BatchTextEmbeddingResult result)
            throws ApiException, NoApiKeyException {
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        result = textEmbedding.fetch(result, null);
        System.out.println(result);
    }

    // 等待任务结束，wait内部封装了轮询逻辑，会一直等待任务结束
    public static void waitTask(BatchTextEmbeddingResult result)
            throws ApiException, NoApiKeyException {
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        result = textEmbedding.wait(result, null);
        System.out.println(result);
    }

    // 取消任务，只能取消处于pending状态的任务
    public static void cancelTask(BatchTextEmbeddingResult result)
            throws ApiException, NoApiKeyException {
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        result = textEmbedding.cancel(result, null);
        System.out.println(result);
    }

    // 查询已经提交的任务
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
