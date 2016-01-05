/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.datamining.bus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * @author Pavol Loffay
 */
public class UrlUtils {

    public static String encodeUrlPath(String url) {
        String result = null;

        try {
            result = URLEncoder.encode(url, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            //todo throw
        }

        return result;
    }

    public static Request buildJsonRequest(String url, Map<String, String> headers) {

        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json");

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                reqBuilder.addHeader(header.getKey(), header.getValue());
            }
        }

        return reqBuilder.get().build();
    }

    public static  <T> T execute(Request request, TypeReference<T> clazz, OkHttpClient client,
                                 ObjectMapper objectMapper) {

        T obj = null;
        try {
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                return null;
            }

            String responseBody = response.body().string();
            obj = objectMapper.readValue(responseBody, clazz);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Request failed: " + request.toString(), ex);
        }

        return obj;
    }
}
