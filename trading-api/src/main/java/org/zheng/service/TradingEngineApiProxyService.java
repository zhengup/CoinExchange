package org.zheng.service;


import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zheng.ApiException;
import org.zheng.enums.ApiError;
import org.zheng.support.LoggerSupport;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class TradingEngineApiProxyService extends LoggerSupport {

    @Value("#{exchangeConfiguration.apiEndpoints.tradingEngineApi}")
    private String tradingEngineInternalApiEndpoint;

    //高性能HTTP客户端，高并发低延迟，快速失败
    private OkHttpClient okhttpClient = new OkHttpClient.Builder()
            // set connect timeout:
            .connectTimeout(1, TimeUnit.SECONDS)
            // set read timeout:
            .readTimeout(1, TimeUnit.SECONDS)
            // set connection pool:
            .connectionPool(new ConnectionPool(20, 60, TimeUnit.SECONDS))
            // do not retry:
            .retryOnConnectionFailure(false).build();

    // get请求，返回json响应
    public String get(String url) throws IOException {
        Request request = new Request.Builder().url(tradingEngineInternalApiEndpoint + url).header("Accept", "*/*")
                .build();
        try (Response response = okhttpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                logger.error("Internal api failed with code {}: {}", Integer.valueOf(response.code()), url);
                throw new ApiException(ApiError.OPERATION_TIMEOUT, null, "operation timeout.");
            }
            try (ResponseBody body = response.body()) {
                String json = body.string();
                if (json == null || json.isEmpty()) {
                    logger.error("Internal api failed with code 200 but empty response: {}", json);
                    throw new ApiException(ApiError.INTERNAL_SERVER_ERROR, null, "response is empty.");
                }
                return json;
            }
        }
    }
}
