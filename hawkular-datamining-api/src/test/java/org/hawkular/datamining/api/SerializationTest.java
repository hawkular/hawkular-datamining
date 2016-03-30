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

package org.hawkular.datamining.api;

import java.io.IOException;
import java.io.StringWriter;

import org.hawkular.datamining.api.base.DataMiningForecaster;
import org.hawkular.datamining.api.base.DataMiningSubscription;
import org.hawkular.datamining.api.json.ObjectMapperConfig;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.model.MetricType;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Pavol Loffay
 */
public class SerializationTest {

    private ObjectMapper mapper;

    public SerializationTest() {
        this.mapper = new ObjectMapper();
        ObjectMapperConfig.config(mapper);
    }

    @Test
    public void testSubscription() throws IOException {

        DataMiningForecaster forecaster = new DataMiningForecaster(new Metric("tenant", "feed", "foo", 1L, 2L, new
                MetricType("fooType", 2L)));

        Subscription subscription = new DataMiningSubscription(forecaster,
                Subscription.SubscriptionOwner.getAllDefined());

        String json = serialize(subscription);

        Assert.assertTrue(json.contains("forecastingHorizon"));
    }

    private String serialize(Object object) throws IOException {
        StringWriter out = new StringWriter();

        JsonGenerator gen = mapper.getFactory().createGenerator(out);

        gen.writeObject(object);

        gen.close();

        out.close();

        return out.toString();
    }

    private <T> T deserialize(String json, Class<T> type) throws Exception {
        JsonParser parser = mapper.getFactory().createParser(json);

        return parser.readValueAs(type);
    }
}
