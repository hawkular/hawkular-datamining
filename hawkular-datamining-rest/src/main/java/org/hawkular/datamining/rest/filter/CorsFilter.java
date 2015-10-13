/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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

package org.hawkular.datamining.rest.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Pavol Loffay
 */
@WebFilter(asyncSupported = true, urlPatterns = "/*")
public class CorsFilter implements Filter {

    private static final String PREFLIGHT_METHOD = "OPTIONS";

    //Header Names
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    public static final String ORIGIN = "Origin";

    //Default CORS Values
    public static final String DEFAULT_CORS_ACCESS_CONTROL_ALLOW_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";
    public static final String DEFAULT_CORS_ACCESS_CONTROL_ALLOW_HEADERS = "origin,accept,content-type," +
            "hawkular-tenant, authorization,  hawkular-persona";
    public static final String ALLOW_ALL_ORIGIN = "*";


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        //NOT a CORS request
        String requestOrigin = httpRequest.getHeader(ORIGIN);

        if (requestOrigin == null) {
            chain.doFilter(request, response);
            return;
        }

        httpResponse.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        httpResponse.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        httpResponse.addHeader(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_CORS_ACCESS_CONTROL_ALLOW_METHODS);
        httpResponse.addHeader(ACCESS_CONTROL_MAX_AGE, (72 * 60 * 60) + "");

        httpResponse.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, DEFAULT_CORS_ACCESS_CONTROL_ALLOW_HEADERS);

        if (!isPreflightRequest((HttpServletRequest) request)) {
            chain.doFilter(request, response);
        }
    }

    private boolean isPreflightRequest(final HttpServletRequest request) {
        return request.getHeader(ACCESS_CONTROL_REQUEST_METHOD) != null
                && request.getMethod() != null
                && request.getMethod().equalsIgnoreCase(PREFLIGHT_METHOD);

    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void destroy() {
    }


}
