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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hawkular.datamining.api.model.BucketPoint;
import org.hawkular.datamining.api.model.DataPoint;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
@Ignore
public class MetricsStorageAdapterTest {

    @Test
    public void testDataPointsLoading() {

        MetricsStorageAdapter metricsStorageAdapter = new MetricsStorageAdapter();

        List<DataPoint> dataPoints =  metricsStorageAdapter.loadPoints(SubscriptionManager.HEAP_USED_METRICS,
                SubscriptionManager.TENANT);

        assertThat(dataPoints, notNullValue());
    }

    @Test
    public void testBucketDataLoading() {
        MetricsStorageAdapter metricsStorageAdapter = new MetricsStorageAdapter();

        List<BucketPoint> dataPoints =  metricsStorageAdapter.loadBuckets(20, SubscriptionManager.HEAP_USED_METRICS,
                SubscriptionManager.TENANT);

        assertThat(dataPoints, notNullValue());
        assertTrue(dataPoints.size() > 0);
    }
}
