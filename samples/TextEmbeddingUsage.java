import java.util.Arrays;
import java.util.concurrent.Semaphore;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

public final class TextEmbeddingUsage {
    public static void basicCall() throws ApiException, NoApiKeyException{
        TextEmbeddingParam param = TextEmbeddingParam
        .builder()
        .model(TextEmbedding.Models.TEXT_EMBEDDING_V3)
        .dimension(128)
        .outputType(TextEmbeddingParam.OutputType.DENSE)
        .instruct("Given a web search query, retrieve relevant passages that answer the query")
        .texts(Arrays.asList("风急天高猿啸哀", "渚清沙白鸟飞回", "无边落木萧萧下", "不尽长江滚滚来")).build();
        TextEmbedding textEmbedding = new TextEmbedding();
        TextEmbeddingResult result = textEmbedding.call(param);
        System.out.println(JsonUtils.gson.toJson(result));
    }
  
    public static void callWithCallback() throws ApiException, NoApiKeyException, InterruptedException{
        TextEmbeddingParam param = TextEmbeddingParam
        .builder()
        .model(TextEmbedding.Models.TEXT_EMBEDDING_V1)
        .texts(Arrays.asList("风急天高猿啸哀", "渚清沙白鸟飞回", "无边落木萧萧下", "不尽长江滚滚来")).build();
        TextEmbedding textEmbedding = new TextEmbedding();
        Semaphore sem = new Semaphore(0);
        textEmbedding.call(param, new ResultCallback<TextEmbeddingResult>() {

          @Override
          public void onEvent(TextEmbeddingResult message) {
            System.out.println(message);
          }
          @Override
          public void onComplete(){
            sem.release();
          }

          @Override
          public void onError(Exception err){
            System.out.println(err.getMessage());
            err.printStackTrace();
            sem.release();
          }
          
        });
        sem.acquire();
    }

  public static void main(String[] args){
//    try{
//      callWithCallback();
//    }catch(ApiException|NoApiKeyException|InterruptedException e){
//      e.printStackTrace();
//      System.out.println(e);
//
//    }
      try {
        basicCall();
    } catch (ApiException | NoApiKeyException e) {
        System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
