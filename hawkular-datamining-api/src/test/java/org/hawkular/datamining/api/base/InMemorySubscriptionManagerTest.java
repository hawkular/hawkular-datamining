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

package org.hawkular.datamining.api.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.exception.SubscriptionNotFoundException;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.model.MetricDataType;
import org.hawkular.datamining.api.model.MetricType;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Forecaster;
import org.hawkular.datamining.forecast.models.Model;
import org.hawkular.datamining.forecast.models.TripleExponentialSmoothing;
import org.hawkular.datamining.forecast.stats.InformationCriterion;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class InMemorySubscriptionManagerTest {
    private static final String tenant = "tenant";
    private static final String metricId = "metricId";

    @Test
    public void testSubscribes() {
        SubscriptionManager subscriptionManager = new InMemorySubscriptionManager(new EmptyMetricsClient());

        Subscription expected = createSubscription(tenant, metricId);
        subscriptionManager.subscribe(expected);
        Subscription got = subscriptionManager.subscription(tenant, metricId);
        Assert.assertEquals(expected, got);

        expected = createSubscription(tenant, "metric2");
        subscriptionManager.subscribe(expected);
        got = subscriptionManager.subscription(tenant, "metric2");
        Assert.assertEquals(expected, got);

        expected = createSubscription("tenant2", "metric1");
        subscriptionManager.subscribe(expected);
        got = subscriptionManager.subscription("tenant2", "metric1");
        Assert.assertEquals(expected, got);
    }

    @Test
    public void testUnsubscribe() {
        SubscriptionManager subscriptionManager = new InMemorySubscriptionManager(new EmptyMetricsClient());

        try {
            subscriptionManager.subscription(tenant, metricId);
            Assert.fail();
        } catch (SubscriptionNotFoundException ex) {
            //ok
        }

        Subscription expected = createSubscription(tenant, metricId);
        subscriptionManager.subscribe(expected);
        Subscription got = subscriptionManager.subscription(tenant, metricId);
        Assert.assertEquals(expected, got);

        subscriptionManager.unsubscribe(tenant, metricId, Subscription.SubscriptionOwner.getAllDefined());

        Assert.assertFalse(subscriptionManager.isSubscribed(tenant, metricId));
        try {
            subscriptionManager.subscription(tenant, metricId);
            Assert.fail();
        } catch (SubscriptionNotFoundException ex) {
            //ok
        }
    }

    @Test
    public void testModelChange() {
        Subscription subscription = createSubscription(tenant, metricId);
        SubscriptionManager subscriptionManager = new InMemorySubscriptionManager(new EmptyMetricsClient());
        subscriptionManager.subscribe(subscription);

        // learn
        subscription.forecaster().learn(generateDataPoints(100));
        Assert.assertNotNull(subscription.forecaster().model());

        Forecaster.Update forecasterUpdate = new Forecaster.Update(60, Model.TripleExponentialSmoothing,
                InformationCriterion.BIC, new AutomaticForecaster.PeriodicIntervalStrategy(60), 2);

        subscription.forecaster().update(forecasterUpdate);
        Assert.assertTrue(subscription.forecaster().model() instanceof TripleExponentialSmoothing);
        Assert.assertEquals(new Integer(2), subscription.forecaster().config().getPeriod());
    }

    public static List<DataPoint> generateDataPoints(int size) {
        List<DataPoint> generatedDataPoints = new ArrayList<>();

        Random random = new Random();
        int min = 0;
        int max = 100;

        for (long i = 0; i < size; i++) {
            double value = min  + (max - min)*random.nextDouble();
            generatedDataPoints.add(new DataPoint(value, i));
        }

        return generatedDataPoints;
    }

    private Subscription createSubscription(String tenant, String metricId) {
        Metric metric = new Metric(tenant, "feed", metricId, 1L, 1L, new MetricType("metricTypeId", 1L,
                MetricDataType.GAUGE));

        Subscription subscription = new DataMiningSubscription(new DataMiningForecaster(metric),
                Collections.singleton(Subscription.SubscriptionOwner.Metric));

        return subscription;
    }
}
