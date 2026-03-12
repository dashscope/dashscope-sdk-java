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
        List<String> referenceUrls = new ArrayList<>();
        referenceUrls.add("https://cdn.wanx.aliyuncs.com/wanx/1014827220770308/upload-video-cut/cda0f4dc063ec258184263691558af36.mp4");

        List<String> referenceVideoDescription = new ArrayList<>();
        referenceVideoDescription.add("这段视频展示一位年轻女性（<cast>）身着灰色长袖上衣与裤子，乌黑长发垂落，面容清秀。她先低头后抬头，目光侧移，继而转身背对再面向镜头，动作流畅自然。背景为素净灰色墙面，环境简约无装饰。镜头由面部特写缓缓拉远至全身，光影柔和，突出人物形态与情绪。");
        VideoSynthesisParam param =
                VideoSynthesisParam.builder()
                        .model("wan2.6-r2v")
                        .prompt(" character1 站在海边，吹着海风，夕阳西下，阳光洒在她的脸上")
                        .referenceUrls(referenceUrls)
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
     * Create a video compositing task and wait for the task to complete.
     */
    public static void basicCallI2V27() throws ApiException, NoApiKeyException, InputRequiredException {
        VideoSynthesis vs = new VideoSynthesis();
        final String prompt = "一幅都市奇幻艺术的场景。一个充满动感的涂鸦艺术角色。一个由喷漆所画成的少年，正从一面混凝土墙上活过来。他一边用极快的语速演唱一首英文rap，一边摆着一个经典的、充满活力的说唱歌手姿势。场景设定在夜晚一个充满都市感的铁路桥下。灯光来自一盏孤零零的街灯，营造出电影般的氛围，充满高能量和惊人的细节。视频的音频部分完全由他的rap构成，没有其他对话或杂音。";
        final String negativePrompt = "ugly, bad anatomy";
        List<VideoSynthesisParam.Media> media = new ArrayList<VideoSynthesisParam.Media>(){{
            add(VideoSynthesisParam.Media.builder()
                    .url("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250925/wpimhv/rap.png")
                    .type(VideoSynthesis.MediaType.FIRST_CLIP)
                    .build());
            add(VideoSynthesisParam.Media.builder()
                    .url("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250925/ozwpvi/rap.mp3")
                    .type(VideoSynthesis.MediaType.DRIVING_AUDIO)
                    .build());
        }};
        VideoSynthesisParam param =
                VideoSynthesisParam.builder()
                        .model("wan2.7-i2v")
                        .prompt(prompt)
                        .media(media)
                        .watermark(Boolean.TRUE)
                        .duration(10)
                        .negativePrompt(negativePrompt)
                        .size("1280*720")
                        .build();
        VideoSynthesisResult result = vs.call(param);
        System.out.println(result);
    }
    /**
     * Create a video compositing task and wait for the task to complete.
     */
    public static void basicCallR2V27() throws ApiException, NoApiKeyException, InputRequiredException {
        VideoSynthesis vs = new VideoSynthesis();
        final String prompt = "一幅都市奇幻艺术的场景。一个充满动感的涂鸦艺术角色。一个由喷漆所画成的少年，正从一面混凝土墙上活过来。他一边用极快的语速演唱一首英文rap，一边摆着一个经典的、充满活力的说唱歌手姿势。场景设定在夜晚一个充满都市感的铁路桥下。灯光来自一盏孤零零的街灯，营造出电影般的氛围，充满高能量和惊人的细节。视频的音频部分完全由他的rap构成，没有其他对话或杂音。";
        final String negativePrompt = "ugly, bad anatomy";
        List<VideoSynthesisParam.Media> media = new ArrayList<VideoSynthesisParam.Media>(){{
            add(VideoSynthesisParam.Media.builder()
                    .url("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/image/res240_269.jpg")
                    .type(VideoSynthesis.MediaType.REFERENCE_IMAGE)
                    .build());
            add(VideoSynthesisParam.Media.builder()
                    .url("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/image/man_5K_7_7K_18_4M.JPG")
                    .referenceVoice("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/audio/2s.wav")
                    .type(VideoSynthesis.MediaType.REFERENCE_IMAGE)
                    .build());
            add(VideoSynthesisParam.Media.builder()
                    .url("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/video/resources/cast/100M.mov")
                    .referenceVoice("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/audio/mp3_1s.mp3")
                    .type(VideoSynthesis.MediaType.REFERENCE_VIDEO)
                    .build());
            add(VideoSynthesisParam.Media.builder()
                    .url("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/video/resources/cast/29_99s.mp4")
                    .referenceDescription("这是一个身穿蓝衣的男子<cast>,他有着浓密的络腮胡")
                    .type(VideoSynthesis.MediaType.REFERENCE_VIDEO)
                    .build());
            add(VideoSynthesisParam.Media.builder()
                    .url("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/video/resources/cast/cat_127.mp4")
                    .referenceVoice("https://test-data-center.oss-accelerate.aliyuncs.com/wanx/audio/wav_10s.wav")
                    .referenceDescription("这是一只毛绒小猫<cast>,它正在对着镜头微笑")
                    .type(VideoSynthesis.MediaType.REFERENCE_VIDEO)
                    .build());
        }};
        VideoSynthesisParam param =
                VideoSynthesisParam.builder()
                        .model("wan2.7-r2v")
                        .prompt(prompt)
                        .media(media)
                        .watermark(Boolean.TRUE)
                        .duration(10)
                        .negativePrompt(negativePrompt)
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
//            basicCall();
//            basicCallI2V27();
            basicCallR2V27();
            // listTask();
            // fetchTask("b451725d-c48f-4f08-9d26-xxx-xxx");
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        System.exit(0);
    }
}
