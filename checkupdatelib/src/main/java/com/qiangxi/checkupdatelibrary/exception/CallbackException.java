package com.qiangxi.checkupdatelibrary.exception;
/*
 * Copyright © qiangxi(任强强)
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

/**
 * Created by qiangxi(任强强) on 2017/10/15.
 * callback类型转换异常
 */

public class CallbackException extends RuntimeException {

    public CallbackException(String message) {
        super(message);
    }

    public CallbackException(Throwable cause) {
        super(cause);
    }

    public CallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
