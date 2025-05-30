// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisListResult;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskListParam;

public class VideoSynthesisUsage {
    /**
     * Create a video compositing task and wait for the task to complete.
     */
    public static void basicCall() throws ApiException, NoApiKeyException, InputRequiredException {
        VideoSynthesis vs = new VideoSynthesis();
        VideoSynthesisParam param =
                VideoSynthesisParam.builder()
                        .model(VideoSynthesis.Models.WANX_2_1_I2V_TURBO)
                        // prompt not required
                        // .prompt("一只戴着绿色眼镜的小狗")
//                        .imgUrl("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/dog.jpeg")
                        .imgUrl("file:///Users/xxx/Documents/source/dog.jpeg")
                        .build();
        VideoSynthesisResult result = vs.call(param);
        System.out.println(result);
    }

    /**
     * List all tasks.
     */
    public static void listTask() throws ApiException, NoApiKeyException {
        VideoSynthesis is = new VideoSynthesis();
        AsyncTaskListParam param = AsyncTaskListParam.builder().build();
        VideoSynthesisListResult result = is.list(param);
        System.out.println(result);
    }

    /**
     * Fetch a task.
     */
    public static void fetchTask(String taskId) throws ApiException, NoApiKeyException {
        // String taskId = "your task id";
        VideoSynthesis is = new VideoSynthesis();
        VideoSynthesisResult result = is.fetch(taskId, null);
        System.out.println(result.getOutput());
        System.out.println(result.getUsage());
    }

    public static void main(String[] args) {
        try {
            basicCall();
            // listTask();
            // fetchTask("b451725d-c48f-4f08-9d26-xxx-xxx");
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
