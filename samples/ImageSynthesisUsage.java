// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.Arrays;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisListResult;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskListParam;

public class ImageSynthesisUsage {
  public static void basicCall() throws ApiException, NoApiKeyException {
    // create with image2image, 参考文档(image2image|text2image)
    ImageSynthesis is = new ImageSynthesis();
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model(ImageSynthesis.Models.WANX_2_1_IMAGEEDIT)
            .size("1024*1024")
            .prompt("帮我编辑图片。把所选区域变成黑色")
            .baseImageUrl("https://static.dingtalk.com/media/lQLPD2jl0mg85BvNBADNAkCwNJvjWJXBVMwHqrO0OvZlAA_576_1024.png_620x10000q90.png")
            .maskImageUrl("https://static.dingtalk.com/media/lQLPD2ob9dfKPBvNBADNAkCwOcPjjaFVcEcHqrO8n1BLAA_576_1024.png_620x10000q90.png")
            .function(ImageSynthesis.ImageEditFunction.DESCRIPTION_EDIT_WITH_MASK)
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
