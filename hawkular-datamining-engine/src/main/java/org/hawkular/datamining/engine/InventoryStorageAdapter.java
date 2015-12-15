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

package org.hawkular.datamining.engine;

import java.io.IOException;
import java.util.Collections;

import org.hawkular.datamining.api.storage.InventoryStorage;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.json.InventoryJacksonConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

/**
 * @author Pavol Loffay
 */
public class InventoryStorageAdapter implements InventoryStorage {
    private  String INVENTORY_BASE_URL;

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    public InventoryStorageAdapter() {
        this.okHttpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();

        try {
            EngineConfiguration configuration = new EngineConfiguration();
            this.INVENTORY_BASE_URL = configuration.getProperty("hawkular.address") +
                    configuration.getProperty("hawkular.inventory.address");
        } catch (IOException ex) {
            // todo
        }

        InventoryJacksonConfig .configure(this.objectMapper);
    }

    public org.hawkular.datamining.api.model.Metric getMetricDefinition(String tenant, String metricId, String feed) {

        String url = INVENTORY_BASE_URL +
                        "/feeds/" + feed +
                        "/metrics/" + UrlUtils.encodeUrlPath(metricId);

        Request request = UrlUtils.buildJsonRequest(url, Collections.emptyMap());

        Metric inventoryMetric = UrlUtils.execute(request, new TypeReference<Metric>(){}, okHttpClient,
                objectMapper);

//        org.hawkular.datamining.api.model.Metric metric =
//                new org.hawkular.datamining.api.model.Metric(tenant, metricId, inventoryMetric.getCollectionInterval()
//                , new MetricType(inventoryMetric.getType().getCollectionInterval()));
        return null;
    }
}
