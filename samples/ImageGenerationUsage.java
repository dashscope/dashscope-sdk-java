import com.alibaba.dashscope.aigc.imagegeneration.*;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;

public class ImageGenerationUsage {

    private static final String DASHSCOPE_API_KEY = System.getenv("DASHSCOPE_API_KEY");

    static ImageGenerationMessage t2iMessage = ImageGenerationMessage.builder()
            .role(Role.USER.getValue())
            .content(Collections.singletonList(
                    Collections.singletonMap("text", "一间有着精致窗户的花店，漂亮的木质门，摆放着花朵")
            )).build();

    public static void t2iUsage() throws NoApiKeyException, UploadFileException{
        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(DASHSCOPE_API_KEY)
                .model(ImageGeneration.Models.WanX2_6_T2I)
                .n(3)
                .messages(Collections.singletonList(t2iMessage))
                .build();

        ImageGeneration ig = new ImageGeneration();
        ImageGenerationResult res =ig.call(param);
        System.out.println(res);
    }

    public static void imageUsage() throws NoApiKeyException, UploadFileException{
        ImageGenerationMessage userMessage = ImageGenerationMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        // ---------------
                        // image 支持本地文件
                        // ---------------
                        Collections.singletonMap("text", "参考图1的风格和图2的背景，生成番茄炒蛋"),
                        Collections.singletonMap("image", "https://cdn.wanx.aliyuncs.com/tmp/pressure/umbrella1.png"),
                        Collections.singletonMap("image", "https://img.alicdn.com/imgextra/i3/O1CN01SfG4J41UYn9WNt4X1_!!6000000002530-49-tps-1696-960.webp")
                )).build();
        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(DASHSCOPE_API_KEY)
                .model(ImageGeneration.Models.WanX2_6_IMAGE)
                .n(1)
                .messages(Collections.singletonList(userMessage))
                .build();

        ImageGeneration ig = new ImageGeneration();
        ImageGenerationResult res =ig.call(param);
        System.out.println(res);
    }


    public static void t2iUsageAsync() throws NoApiKeyException, UploadFileException{
        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(DASHSCOPE_API_KEY)
                .model(ImageGeneration.Models.WanX2_6_T2I)
                .n(1)
                .messages(Collections.singletonList(t2iMessage))
                .build();

        ImageGeneration ig = new ImageGeneration();
        ImageGenerationResult res = ig.asyncCall(param);
        System.out.println(res);

        String taskId = res.getOutput().getTaskId();
        testAsyncTask(taskId);
    }

    public static void testAsyncTask(String taskId) throws NoApiKeyException {
        ImageGeneration ig = new ImageGeneration();
        System.out.println();
        System.out.println();
        System.out.println("-----------async-t2i-fetch-res-----------");
        ImageGenerationResult res = ig.fetch(taskId, DASHSCOPE_API_KEY);
        System.out.println(res);

        try {
            System.out.println();
            System.out.println();
            System.out.println("-----------async-t2i-cancel-res-----------");
            res = ig.cancel(taskId, DASHSCOPE_API_KEY);
            System.out.println(res);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        System.out.println();
        System.out.println();
        System.out.println("-----------async-t2i-wait-res-----------");
        res = ig.wait(taskId, DASHSCOPE_API_KEY);
        System.out.println(res);

        AsyncTaskListParam param = AsyncTaskListParam.builder().build();
        System.out.println();
        System.out.println();
        System.out.println("-----------async-task-list-res-----------");
        ImageGenerationListResult res2 = ig.list(param);
        System.out.println(res2);
    }

    public static void imageUsageStream() throws NoApiKeyException, UploadFileException{
        ImageGenerationMessage userMessage = ImageGenerationMessage.builder()
                .role(Role.USER.getValue())
                .content(Collections.singletonList(
                        Collections.singletonMap("text", "给我一个1张图辣椒炒肉教程")
                )).build();
        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(DASHSCOPE_API_KEY)
                .model(ImageGeneration.Models.WanX2_6_IMAGE)
                .messages(Collections.singletonList(userMessage))
                .stream(true)
                .enableInterleave(true)
                .maxImages(1)
                .build();

        ImageGeneration ig = new ImageGeneration();
        Flowable<ImageGenerationResult> res = ig.streamCall(param);
        res.blockingForEach(System.out::println);
    }

    public static void main(String[] args) {
        try {
        t2iUsage();
//        imageUsage();
//        t2iUsageAsync();
//        imageUsageStream();
        }catch (NoApiKeyException | UploadFileException e){
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }

}
