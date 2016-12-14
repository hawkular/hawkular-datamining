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

package org.hawkular.datamining.itest;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.hawkular.datamining.api.Constants;
import org.hawkular.datamining.api.json.ObjectMapperConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractITest {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    public static final String host;
    public static final String baseURI;
    public static final int port;
    static {
        String hostProp = System.getProperty("hawkular.bind.address") == null ? "localhost" :
                System.getProperty("hawkular.bind.address");
        if ("0.0.0.0".equals(hostProp)) {
            hostProp = "localhost";
        }

        int portOffset = System.getProperty("hawkular.port.offset") == null ? 0 :
                Integer.parseInt(System.getProperty("hawkular.port.offset"));

        host = hostProp;
        port = portOffset + 8080;
        baseURI = "http://" + host + ":" + port + "/hawkular/datamining/";
    }

    private final ObjectMapper mapper;
    private final OkHttpClient client;

    protected AbstractITest() {
        mapper = new ObjectMapper();
        ObjectMapperConfig.config(mapper);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    protected Response get(String path) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(baseURI + path)
                .addHeader("Accept", "application/json")
                .build();

        return execute(request);
    }

    protected Response get(String path, String tenant) throws IOException {
        Request request = new Request.Builder()
                .url(baseURI + path)
                .addHeader("Accept", "application/json")
                .addHeader(Constants.TENANT_HEADER_NAME, tenant)
                .build();

        return execute(request);
    }

    protected Response post(String path, String tenant, String payload) throws Throwable {
        Request request = new Request.Builder()
                .post(RequestBody.create(MEDIA_TYPE_JSON, payload))
                .url(baseURI + path)
                .addHeader(Constants.TENANT_HEADER_NAME, tenant)
                .build();

        return execute(request);
    }

    protected Response post(String path, String tenant, Object payload) throws Throwable {
        String json = mapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .post(RequestBody.create(MEDIA_TYPE_JSON, json))
                .url(baseURI + path)
                .addHeader(Constants.TENANT_HEADER_NAME, tenant)
                .build();

        return execute(request);
    }

    protected Response put(String path, String tenant, Object payload) throws Throwable {
        String json = mapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .put(RequestBody.create(MEDIA_TYPE_JSON, json))
                .url(baseURI + path)
                .addHeader(Constants.TENANT_HEADER_NAME, tenant)
                .build();

        return execute(request);
    }

    protected Response postNewEntity(String path, String tenant, Object payload) throws Throwable {
        String json = mapper.writeValueAsString(payload);
        Response response = post(path, tenant, json);
        assertThat(response.code(), is(201));
        return response;
    }

    protected <T> T parseResponseBody(Response response, TypeReference<T> type) throws IOException {

        String json = response.body().string();

        T result =  mapper.readValue(json, type);

        return result;
    }

    private Response execute(Request request) throws IOException {
        System.out.format("---> Request: %s\n", request);
        Response response = client.newCall(request).execute();
        System.out.format("<--- Response: %s\n", response.toString());
        return response;
    }
}
