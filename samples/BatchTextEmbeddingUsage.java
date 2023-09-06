import com.alibaba.dashscope.embeddings.BatchTextEmbedding;
import com.alibaba.dashscope.embeddings.BatchTextEmbeddingParam;
import com.alibaba.dashscope.embeddings.BatchTextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.alibaba.dashscope.task.AsyncTaskListResult;
import com.alibaba.dashscope.utils.JsonUtils;

public class BatchTextEmbeddingUsage {
    private static final String model = "text-embedding-async-v1";
    public static void basicCall() throws ApiException, NoApiKeyException{
        BatchTextEmbeddingParam param = BatchTextEmbeddingParam
        .builder()
        .model(model)
        .url("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt").build();
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        BatchTextEmbeddingResult result = textEmbedding.call(param);
        System.out.println(result);
    }
    public static void list() throws ApiException, NoApiKeyException{
        AsyncTaskListParam param = AsyncTaskListParam.builder().pageNo(1).pageSize(20)
        .build();
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        AsyncTaskListResult result = textEmbedding.list(param);
        System.out.println(JsonUtils.toJson(result));
    }

    public static void asyncCallFetch() throws ApiException, NoApiKeyException{
        BatchTextEmbeddingParam param = BatchTextEmbeddingParam
        .builder()
        .model(model)
        .url("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt").build();
        BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
        BatchTextEmbeddingResult result = textEmbedding.asyncCall(param);
        System.out.println(result);        
        result = textEmbedding.wait(result, null);
        System.out.println(result);
    }

    public static void main(String[] args){
        try {
            basicCall();
            list();
        } catch (ApiException | NoApiKeyException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
