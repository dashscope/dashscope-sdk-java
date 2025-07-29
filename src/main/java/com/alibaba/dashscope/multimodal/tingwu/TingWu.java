package com.alibaba.dashscope.multimodal.tingwu;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;

/**
 * The tingwu client.
 */
public final class TingWu {
    private final SynchronizeHalfDuplexApi<HalfDuplexServiceParam> syncApi;
    private final ApiServiceOption serviceOption;
    private final String DEFAULT_BASE_HTTP_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private ApiServiceOption defaultApiServiceOption() {
        return ApiServiceOption.builder()
                .protocol(Protocol.HTTP)
                .httpMethod(HttpMethod.POST)
                .isService(false)
                .baseHttpUrl(DEFAULT_BASE_HTTP_URL)
                .build();
    }

    public TingWu() {
        serviceOption = defaultApiServiceOption();
        syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
    }

    public TingWu(String protocol) {
        serviceOption = defaultApiServiceOption();
        syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
    }

    public TingWu(String protocol, String baseUrl) {
        serviceOption = defaultApiServiceOption();
        serviceOption.setProtocol(Protocol.of(protocol));
        if (protocol.equals(Protocol.HTTP.getValue())) {
            serviceOption.setBaseHttpUrl(baseUrl);
        } else {
            serviceOption.setBaseWebSocketUrl(baseUrl);
        }
        syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
    }

    public TingWu(String protocol, String baseUrl, ConnectionOptions connectionOptions) {
        serviceOption = defaultApiServiceOption();
        serviceOption.setProtocol(Protocol.of(protocol));
        if (protocol.equals(Protocol.HTTP.getValue())) {
            serviceOption.setBaseHttpUrl(baseUrl);
        } else {
            serviceOption.setBaseWebSocketUrl(baseUrl);
        }
        syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
    }


    /**
     * Call the server to get the whole result, only http protocol
     */
    public DashScopeResult call(HalfDuplexServiceParam param)
            throws ApiException, NoApiKeyException, InputRequiredException {
        param.validate();
        serviceOption.setIsSSE(false);
        return syncApi.call(param);
    }

}
