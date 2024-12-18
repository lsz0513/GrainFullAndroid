/*
 * Copyright 2017 JessYan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zqw.mobile.grainfull.app.config;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jess.arms.http.GlobalHttpHandler;
import com.zqw.mobile.grainfull.app.global.AccountManager;
import com.zqw.mobile.grainfull.app.global.Constant;
import com.zqw.mobile.grainfull.app.utils.CommonUtils;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ================================================
 * 展示 {@link GlobalHttpHandler} 的用法
 * <p>
 * Created by JessYan on 04/09/2017 17:06
 * ================================================
 */
public class GlobalHttpHandlerImpl implements GlobalHttpHandler {
    private Context context;
    private AccountManager mAccountManager;

    public GlobalHttpHandlerImpl(Context context) {
        this.context = context;
        mAccountManager = new AccountManager(context);
    }

    /**
     * 这里可以先客户端一步拿到每一次 Http 请求的结果, 可以先解析成 Json, 再做一些操作, 如检测到 token 过期后
     * 重新请求 token, 并重新执行请求
     *
     * @param httpResult 服务器返回的结果 (已被框架自动转换为字符串)
     * @param chain      {@link Interceptor.Chain}
     * @param response   {@link Response}
     * @return {@link Response}
     */
    @NonNull
    @Override
    public Response onHttpResultResponse(@Nullable String httpResult, @NonNull Interceptor.Chain chain, @NonNull Response response) {
        /* 这里可以先客户端一步拿到每一次 Http 请求的结果, 可以先解析成 Json, 再做一些操作, 如检测到 token 过期后重新请求 token, 并重新执行请求 */
//        if (!TextUtils.isEmpty(httpResult) && RequestInterceptor.isJson(response.body().contentType())) {
//            try {
//                List<User> list = ArmsUtils.obtainAppComponentFromContext(context).gson().fromJson(httpResult, new TypeToken<List<User>>() {
//                }.getType());
//                User user = list.get(0);
//                Timber.w("Result ------> " + user.getLogin() + "    ||   Avatar_url------> " + user.getAvatarUrl());
//            } catch (Exception e) {
//                e.printStackTrace();
//                return response;
//            }
//        }

        /* 这里如果发现 token 过期, 可以先请求最新的 token, 然后在拿新的 token 放入 Request 里去重新请求
        注意在这个回调之前已经调用过 proceed(), 所以这里必须自己去建立网络请求, 如使用 Okhttp 使用新的 Request 去请求
        create a new request and modify it accordingly using the new token
        Request newRequest = chain.request().newBuilder().header("token", newToken)
                             .build();

        retry the request

        response.body().close();
        如果使用 Okhttp 将新的请求, 请求成功后, 再将 Okhttp 返回的 Response return 出去即可
        如果不需要返回新的结果, 则直接把参数 response 返回出去即可*/
        return response;
    }

    /**
     * 这里可以在请求服务器之前拿到 {@link Request}, 做一些操作比如给 {@link Request} 统一添加 token 或者 header 以及参数加密等操作
     *
     * @param chain   {@link Interceptor.Chain}
     * @param request {@link Request}
     * @return {@link Request}
     */
    @NonNull
    @Override
    public Request onHttpRequestBefore(@NonNull Interceptor.Chain chain, @NonNull Request request) {
        if (request.url().toString().contains("image-process/v1/selfie_anime")) {
            // 百度接口做特殊处理
            return chain.request().newBuilder().addHeader("Content-Type", "application/x-www-form-urlencoded").addHeader("Accept", "application/json").build();
        } else if (request.url().toString().contains("image-process/v1/doc_repair")) {
            // 百度接口做特殊处理
            return chain.request().newBuilder().addHeader("Content-Type", "application/x-www-form-urlencoded").build();
        } else if (request.url().toString().contains(Constant.CHATGPT_CHAT_URL)) {
            // ChatGPT - 文字对话
            return chain.request().newBuilder().addHeader("Content-Type", "application/json;charset=UTF-8").addHeader("Authorization", "Bearer " + mAccountManager.getChatGptSk()).build();
        } else if (request.url().toString().contains(Constant.CHATGPT_IMAGE_URL)) {
            // ChatGPT - 图片绘制
            return chain.request().newBuilder().addHeader("Content-Type", "application/json;charset=UTF-8").addHeader("Authorization", "Bearer " + mAccountManager.getChatGptSk()).build();
        } else if (request.url().toString().contains(Constant.CHATGPT_TOKEN)) {
            // ChatGPT - 查询令牌余额
            return chain.request().newBuilder().addHeader("Content-Type", "application/json;charset=UTF-8").build();
        } else if (request.url().toString().contains(Constant.CHATGPT_TRANSCRIPTIONS_URL)) {
            // ChatGPT - 语音转文字
            return chain.request().newBuilder().addHeader("Authorization", "Bearer " + mAccountManager.getChatGptSk()).build();
        } else if (request.url().toString().contains(Constant.CHATGPT_SPEECH_URL)) {
            // ChatGPT - 文字转语音
            return chain.request().newBuilder().addHeader("Content-Type", "application/json;charset=UTF-8").addHeader("Authorization", "Bearer " + mAccountManager.getChatGptSk()).build();
        } else if (request.url().toString().contains(Constant.FASTGPT_CHAT_URL)) {
            // FastGPT - 文字对话
            return chain.request().newBuilder().addHeader("Content-Type", "application/json;charset=UTF-8").addHeader("Authorization", "Bearer " + mAccountManager.getFastGptSk()).build();
        } else if (request.url().toString().contains(Constant.FASTGPT_TRANSCRIPTIONS_URL)) {
            // FastGPT - 语音转文字
            return chain.request().newBuilder().addHeader("Authorization", "Bearer " + mAccountManager.getFastGptSk()).build();
        } else if (request.url().toString().contains(Constant.FASTGPT_SPEECH_URL)) {
            // FastGPT - 文字转语音
            return chain.request().newBuilder().addHeader("Content-Type", "application/json;charset=UTF-8").addHeader("Authorization", "Bearer " + mAccountManager.getFastGptSk()).build();
        } else if (request.url().toString().contains(Constant.FASTGPT_HISTORY_URL)) {
            // FastGPT - 获取历史记录
            return chain.request().newBuilder().addHeader("Content-Type", "application/json;charset=UTF-8").addHeader("token",mAccountManager.getFastGptToken()).build();
        } else {
            /* 如果需要在请求服务器之前做一些操作, 则重新构建一个做过操作的 Request 并 return, 如增加 Header、Params 等请求信息, 不做操作则直接返回参数 request */
            return chain.request().newBuilder().header("token", CommonUtils.isEmptyReturnStr(mAccountManager.getToken())).header("Content-Type", "application/json;charset=UTF-8").build();
        }

//        return request;
    }
}
