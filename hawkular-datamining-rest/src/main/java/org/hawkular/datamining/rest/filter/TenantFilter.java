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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.hawkular.datamining.api.Constants;
import org.hawkular.datamining.rest.RestPing;

/**
 * @author Pavol Loffay
 */
@Provider
public class TenantFilter implements ContainerRequestFilter {

    private static final String MISSING_TENANT_MSG;
    private static final Set<String> pathsWithoutTenantHeader = new HashSet<>();
    static {
        MISSING_TENANT_MSG = "Tenant is not specified. Use '" + Constants.TENANT_HEADER_NAME + "' header.";

        pathsWithoutTenantHeader.add(RestPing.URL);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        String tenant = requestContext.getHeaders().getFirst(Constants.TENANT_HEADER_NAME);
        if (tenant != null && !tenant.trim().isEmpty()) {
            // We're good already
            return;
        }

        // Some destinations are available without tenant header
        if (pathsWithoutTenantHeader.contains(requestContext.getUriInfo().getPath())) {
            return;
        }

        // Fail on missing tenant info
        Response response = Response.status(Response.Status.BAD_REQUEST)
                .type(APPLICATION_JSON_TYPE)
                .entity(MISSING_TENANT_MSG)
                .build();
        requestContext.abortWith(response);
    }
}
