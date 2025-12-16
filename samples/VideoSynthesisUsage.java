// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisListResult;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskListParam;

import java.util.ArrayList;
import java.util.List;

public class VideoSynthesisUsage {
    /**
     * Create a video compositing task and wait for the task to complete.
     */
    public static void basicCall() throws ApiException, NoApiKeyException, InputRequiredException {
        VideoSynthesis vs = new VideoSynthesis();
        List<String> referenceVideoUrls = new ArrayList<>();
        referenceVideoUrls.add("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/video/resources/with_human_voice_11s.mov");
        VideoSynthesisParam param =
                VideoSynthesisParam.builder()
                        .model("wan2.6-r2v")
                        .prompt("一只小猫在月光下奔跑")
                        .referenceVideoUrls(referenceVideoUrls)
                        .shotType(VideoSynthesis.ShotType.MULTI)
                        .watermark(Boolean.TRUE)
                        .audio(Boolean.TRUE)
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
            e.printStackTrace();
        }
        System.exit(0);
    }
}
