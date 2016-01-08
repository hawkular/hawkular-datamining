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

package org.hawkular.datamining.engine.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.TenantSubscriptions;
import org.hawkular.datamining.api.TimeSeriesLinkedModel;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.engine.EngineLogger;

/**
 * @author Pavol Loffay
 */
public class CombinedTimeSeriesModel implements TimeSeriesLinkedModel {
    // ewma
    public static final double EWMA_ALPHA = 0.005;
    public static final double EWMA_BETA = 0.005;
    // lms
    public static final double[] LMS_WEIGHTS = new double[] {3, -1};
    // TODO this has to be calculated from data, or use normalized version
    public static final double LMS_ALPHA = 0.000000000000000000000000001;

    private final Metric metric;
    private final TenantSubscriptions tenantSubscriptions;
    private Set<SubscriptionManager.SubscriptionOwner> subscriptionOwners = new HashSet<>();

    // in ms
    private long lastTimestamp;

    private LeastMeanSquaresFilter leastMeanSquaresFilter;
    private ExponentiallyWeightedMovingAverages ewma;

    public CombinedTimeSeriesModel(Metric metric, TenantSubscriptions tenantSubscriptions,
                                   Set<SubscriptionManager.SubscriptionOwner> subscriptionOwner) {
        this.metric = metric;
        this.tenantSubscriptions = tenantSubscriptions;
        this.subscriptionOwners.addAll(subscriptionOwner);

        this.ewma = new ExponentiallyWeightedMovingAverages(EWMA_ALPHA, EWMA_BETA);
        this.leastMeanSquaresFilter = new LeastMeanSquaresFilter(LMS_ALPHA, LMS_WEIGHTS);
    }

    @Override
    public Metric getLinkedMetric() {
        return metric;
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        addData(Arrays.asList(dataPoint));
    }

    @Override
    public void addDataPoints(List<DataPoint> dataPoints) {
        addData(dataPoints);
    }

    @Override
    public DataPoint predict() {
        return predict(1).get(0);
    }

    @Override
    public List<DataPoint> predict(int nAhead) {

        if (lastTimestamp == 0) {
            return Collections.EMPTY_LIST;
        }

        Long collectionInterval = getCollectionInterval();
        Long predictionInterval = getPredictionInterval();

        if (nAhead == 0) {
            nAhead = (int) (predictionInterval / collectionInterval);
            EngineLogger.LOGGER.debugf("Automatic prediction, nAhead= %d", nAhead);
        }

        /**
         * Calculate
         * from prediction interval calculate how far - how many aHead we need to predict
         */

        List<DataPoint> result = new ArrayList<>();
        List<DataPoint> predictionEWMA = ewma.predict(nAhead);
        List<DataPoint> predictionFilter = leastMeanSquaresFilter.predict(nAhead);

        for (int i = 0; i < predictionEWMA.size() && i < predictionFilter.size(); i++) {

            double ewma = predictionEWMA.get(i).getValue();
            double filter = predictionFilter.get(i).getValue();

//            EngineLogger.LOGGER.debugf("Filter predicted %f", filter);
//            EngineLogger.LOGGER.debugf("EWMA predicted %f", ewma);

            double mean = (ewma + filter) / 2;
            ewma = ewma - mean;
            filter = filter - mean;

            DataPoint dataPoint = new DataPoint((ewma + filter) + mean,
                    lastTimestamp + i * collectionInterval * 1000);
            result.add(dataPoint);

//            EngineLogger.LOGGER.debugf("Prediction: %s, %s", metric.getTenant(), metric.getId(), dataPoint);
        }

        return result;
    }

    private void addData(List<DataPoint> dataPoints) {
        // todo data from metrics arrives at <new>, <older>. Maybe its better to reverse itterate
        Collections.sort(dataPoints);

        long newLastTimestamp = dataPoints.get(dataPoints.size() - 1).getTimestamp();
        if (newLastTimestamp < lastTimestamp) {
            throw new IllegalArgumentException("Data point has older timestamp than current state.");
        }

        lastTimestamp = newLastTimestamp;

        ewma.addDataPoints(dataPoints);
        leastMeanSquaresFilter.addDataPoints(dataPoints);
    }

    private Long getPredictionInterval() {
        Long predictionInterval = null;

        if (subscriptionOwners.contains(SubscriptionManager.SubscriptionOwner.Metric)) {
            predictionInterval = metric.getPredictionInterval();
        } else if (subscriptionOwners.contains(SubscriptionManager.SubscriptionOwner.MetricType)) {
            predictionInterval = metric.getMetricType().getPredictionInterval();
        } else {
            predictionInterval = tenantSubscriptions.getPredictionInterval();
        }

        return predictionInterval;
    }

    private Long getCollectionInterval() {
        Long collectionInterval = null;

        if (metric.getCollectionInterval() != null) {
            collectionInterval =  metric.getCollectionInterval();
        } else if (metric.getMetricType() != null) {
            collectionInterval = metric.getMetricType().getCollectionInterval();
        }

        return collectionInterval;
    }

    public Set<SubscriptionManager.SubscriptionOwner> getSubscriptionOwners() {
        return new HashSet<>(subscriptionOwners);
    }

    @Override
    public void addSubscriptionOwner(SubscriptionManager.SubscriptionOwner subscriptionOwner) {
        this.subscriptionOwners.add(subscriptionOwner);
    }

    @Override
    public void addAllSubscriptionOwners(Set<SubscriptionManager.SubscriptionOwner> owners) {
        this.subscriptionOwners.addAll(owners);
    }

    @Override
    public void removeSubscriptionOwner(SubscriptionManager.SubscriptionOwner subscriptionOwner) {
        this.subscriptionOwners.remove(subscriptionOwner);
    }
}
