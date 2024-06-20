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
    ImageSynthesis is = new ImageSynthesis("image2image");
    // [[0, 0, 0], [134, 134, 134]]
    ArrayList<ArrayList<Integer>> maskColor = new ArrayList<ArrayList<Integer>>();
    ArrayList<Integer> white = new ArrayList<>();
    white.add(0);
    white.add(0);
    white.add(0);
    ArrayList<Integer> second = new ArrayList<>();
    second.add(134);
    second.add(134);
    second.add(134);
    maskColor.add(white);
    maskColor.add(second);
    // 在input字段中如果有没有定义参数，通过extraInput添加，例如下面添加base_image_url
    // 在parameters字段中没有定义的参数，可以通过parameter添加，如下maskColor
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model("wanx-x-painting")
            .size("1024*1024")
            .prompt("一只戴着绿色眼镜的小狗")
            .extraInput("base_image_url", "https://modelscope.oss-cn-beijing.aliyuncs.com/resource/dog.jpeg")
            .extraInput("mask_image_url", "https://modelscope.oss-cn-beijing.aliyuncs.com/resource/glasses.jpeg")
            .parameter("mask_color", maskColor)
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
