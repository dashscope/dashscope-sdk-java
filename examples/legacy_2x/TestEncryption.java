import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.EncryptionKey;
import com.alibaba.dashscope.utils.EncryptionKeys;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

public class TestEncryption {
    public static void qwenQuickStartStream()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation(Protocol.HTTP.getValue());
        Message userMsg =
                Message.builder().role(Role.USER.getValue()).content("Tell me something about AI.").build();
        GenerationParam param = GenerationParam.builder().model("qwen-plus").resultFormat("message")
                .messages(Arrays.asList(userMsg)).enableEncrypt(true).build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(msg -> {
            System.out.println(msg);
        });
    }

    public static void qwenQuickStartGeneral()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation(Protocol.HTTP.getValue());
        Message userMsg = Message.builder().role(Role.USER.getValue())
                .content("Tell me something about AI.").build();
        GenerationParam param = GenerationParam.builder().model("qwen-turbo").enableEncrypt(true).resultFormat("message")
                .messages(Arrays.asList(userMsg)).build();
        GenerationResult result = gen.call(param);
        System.out.println(JsonUtils.toJson(result));
    }

    /**
     * Get encrypt key from server.
     * @return
     * @throws ApiException
     * @throws NoApiKeyException
     */
    public static EncryptionKey getEncryptKey() throws ApiException, NoApiKeyException {
        EncryptionKeys encryptionKeys = new EncryptionKeys();
        return encryptionKeys.get();
    }

    public static void main(String[] args) throws ApiException, NoApiKeyException,
            InputRequiredException, NoSuchAlgorithmException {
        qwenQuickStartGeneral();
        qwenQuickStartStream();
        System.exit(0);

    }
}
