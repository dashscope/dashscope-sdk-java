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
        referenceVideoUrls.add("https://cdn.wanx.aliyuncs.com/wanx/1014827220770308/upload-video-cut/cda0f4dc063ec258184263691558af36.mp4");

        List<String> referenceVideoDescription = new ArrayList<>();
        referenceVideoDescription.add("这段视频展示一位年轻女性（<cast>）身着灰色长袖上衣与裤子，乌黑长发垂落，面容清秀。她先低头后抬头，目光侧移，继而转身背对再面向镜头，动作流畅自然。背景为素净灰色墙面，环境简约无装饰。镜头由面部特写缓缓拉远至全身，光影柔和，突出人物形态与情绪。");
        VideoSynthesisParam param =
                VideoSynthesisParam.builder()
                        .model("wan2.6-r2v")
                        .prompt(" character1 站在海边，吹着海风，夕阳西下，阳光洒在她的脸上")
                        .referenceVideoUrls(referenceVideoUrls)
                        .referenceVideoDescription(referenceVideoDescription)
                        .shotType(VideoSynthesis.ShotType.MULTI)
                        .watermark(Boolean.TRUE)
                        .audio(Boolean.TRUE)
                        .duration(10)
                        .promptExtend(Boolean.TRUE)
                        .size("1280*720")
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
