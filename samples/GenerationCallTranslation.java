// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.TranslationOptions;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GenerationCallTranslation {
    static final String MODE_NAME = "qwen-mt-turbo";

    public static void baseCall()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        List<Message> msgManager = new ArrayList<>();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("很久很久以前，有一只小猫，猫的名字叫小花。")
                .build();
        msgManager.add(userMsg);
        GenerationParam param =
                GenerationParam.builder()
                        .model(MODE_NAME)
                        .messages(msgManager)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .translationOptions(
                                TranslationOptions.builder()
                                        .sourceLang("Chinese")
                                        .targetLang("English")
                                        .build())
                        .build();
        GenerationResult result = gen.call(param);
        System.out.println(result);
    }

    public static void streamCallTerms()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        List<Message> msgManager = new ArrayList<>();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("而这套生物传感器运用了石墨烯这种新型材料，它的目标物是化学元素，敏锐的“嗅觉”让它能更深度、准确地体现身体健康状况。")
                .build();
        msgManager.add(userMsg);

        TranslationOptions translationOptions = TranslationOptions.builder()
                .sourceLang("Chinese")
                .targetLang("English")
                .terms(Arrays.asList(
                        TranslationOptions.Term.builder()
                                .source("生物传感器")
                                .target("shengwu sensor")
                                .build(),
                        TranslationOptions.Term.builder()
                                .source("石墨烯")
                                .target("graphene")
                                .build(),
                        TranslationOptions.Term.builder()
                                .source("化学元素")
                                .target("huaxue elements")
                                .build(),
                        TranslationOptions.Term.builder()
                                .source("身体健康状况")
                                .target("jiankang status of the body")
                                .build()
                ))
                .build();

        GenerationParam param =
                GenerationParam.builder()
                        .model(MODE_NAME)
                        .messages(msgManager)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .incrementalOutput(true)
                        .translationOptions(translationOptions)
                        .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingSubscribe( data -> {
            System.out.println(JsonUtils.toJson(data));
        });
    }

    public static void streamCallTmList()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        List<Message> msgManager = new ArrayList<>();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("通过如下命令可以看出安装thrift的版本信息；")
                .build();
        msgManager.add(userMsg);

        TranslationOptions translationOptions = TranslationOptions.builder()
                .sourceLang("Chinese")
                .targetLang("English")
                .tmList(Arrays.asList(
                        TranslationOptions.Tm.builder()
                                .source("您可以通过如下方式查看集群的内核版本信息:")
                                .target("You can use one of the following methods to query the engine version of a cluster:")
                                .build(),
                        TranslationOptions.Tm.builder()
                                .source("我们云HBase的thrift环境是0.9.0,所以建议客户端的版本也为 0.9.0,可以从这里下载thrift的0.9.0 版本,下载的源码包我们后面会用到,这里需要先安装thrift编译环境,对于源码安装可以参考thrift官网;")
                                .target("The version of Thrift used by ApsaraDB for HBase is 0.9.0. Therefore, we recommend that you use Thrift 0.9.0 to create a client. Click here to download Thrift 0.9.0. The downloaded source code package will be used later. You must install the Thrift compiling environment first. For more information, see Thrift official website.")
                                .build(),
                        TranslationOptions.Tm.builder()
                                .source("您可以通过PyPI来安装SDK,安装命令如下:")
                                .target("You can run the following command in Python Package Index (PyPI) to install Elastic Container Instance SDK for Python:")
                                .build()
                ))
                .build();

        GenerationParam param =
                GenerationParam.builder()
                        .model(MODE_NAME)
                        .messages(msgManager)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .incrementalOutput(true)
                        .translationOptions(translationOptions)
                        .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingSubscribe( data -> {
            System.out.println(JsonUtils.toJson(data));
        });
    }

    public static void streamCallDomains()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        List<Message> msgManager = new ArrayList<>();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("第二个SELECT语句返回一个数字，表示在没有LIMIT子句的情况下，第一个SELECT语句返回了多少行。")
                .build();
        msgManager.add(userMsg);

        TranslationOptions translationOptions = TranslationOptions.builder()
                .sourceLang("Chinese")
                .targetLang("English")
                .domains("The sentence is from Ali Cloud IT domain. It mainly involves computer-related software development and usage methods, including many terms related to computer software and hardware. Pay attention to professional troubleshooting terminologies and sentence patterns when translating. Translate into this IT domain style.")
                .build();

        GenerationParam param =
                GenerationParam.builder()
                        .model(MODE_NAME)
                        .messages(msgManager)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .incrementalOutput(true)
                        .translationOptions(translationOptions)
                        .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingSubscribe( data -> {
            System.out.println(JsonUtils.toJson(data));
        });
    }

    public static void main(String[] args){
        try {
//            baseCall();
//            streamCallTerms();
//            streamCallTmList();
            streamCallDomains();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
