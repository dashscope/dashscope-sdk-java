// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisListResult;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.base.AsyncTaskListParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;

public class ImageSynthesisUsage {
  public static void basicCall() throws ApiException, NoApiKeyException {
    ImageSynthesis is = new ImageSynthesis();
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model(ImageSynthesis.Models.WANX_V1)
            .n(4)
            .size("1024*1024")
            .prompt("雄鹰自由自在的在蓝天白云下飞翔")
            .build();

    ImageSynthesisResult result = is.call(param);
    System.out.println(result);
  }

  public static void listTask() throws ApiException, NoApiKeyException {
    ImageSynthesis is = new ImageSynthesis();
    AsyncTaskListParam param = AsyncTaskListParam.builder().build();
    ImageSynthesisListResult result = is.list(param);
    System.out.println(result);
  }

  public void fetchTask() throws ApiException, NoApiKeyException {
    String taskId = "your task id";
    ImageSynthesis is = new ImageSynthesis();
    // If set DASHSCOPE_API_KEY environment variable, apiKey can null.
    ImageSynthesisResult result = is.fetch(taskId, null);
    System.out.println(result.getOutput());
    System.out.println(result.getUsage());
  }

  public static void main(String[] args){
    try{
      basicCall();
      //listTask();
    }catch(ApiException|NoApiKeyException e){
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
