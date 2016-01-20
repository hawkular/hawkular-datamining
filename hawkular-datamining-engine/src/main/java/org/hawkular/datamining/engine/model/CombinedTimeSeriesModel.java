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

import org.hawkular.datamining.api.ModelManager;
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
    public static final double EWMA_ALPHA = 0.4;
    public static final double EWMA_BETA = 0.15;
    // lms
    public static final double[] LMS_WEIGHTS = new double[] {3, -1};
    // TODO this has to be calculated from data, or use normalized version
    public static final double LMS_ALPHA = 0.000000000000000000000000001;

    private final Metric metric;
    private final TenantSubscriptions tenantSubscriptions;
    private Set<ModelManager.ModelOwner> modelOwners = new HashSet<>();

    // in ms
    private long lastTimestamp;
    private LeastMeanSquaresFilter leastMeanSquaresFilter;
    private DoubleExponentialSmoothing ewma;


    public CombinedTimeSeriesModel(Metric metric, TenantSubscriptions tenantSubscriptions,
                                   Set<ModelManager.ModelOwner> modelOwner) {
        this.metric = metric;
        this.tenantSubscriptions = tenantSubscriptions;
        this.modelOwners.addAll(modelOwner);

        this.ewma = new DoubleExponentialSmoothing(EWMA_ALPHA, EWMA_BETA);
        this.leastMeanSquaresFilter = new LeastMeanSquaresFilter(LMS_ALPHA, LMS_WEIGHTS);
    }

    @Override
    public Metric getLinkedMetric() {
        return metric;
    }

    @Override
    public void learn(DataPoint dataPoint) {
        addData(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        addData(dataPoints);
    }

    @Override
    public DataPoint predict() {
        List<DataPoint> result = predict(1);

        return result.size() > 0 ? result.get(0) : new DataPoint(0d ,lastTimestamp);
    }

    @Override
    public List<DataPoint> predict(int nAhead) {

        if (lastTimestamp == 0) {
            return Collections.EMPTY_LIST;
        }

        List<DataPoint> result = new ArrayList<>(nAhead);
        List<DataPoint> predictionEWMA = ewma.predict(nAhead);

        for (int i = 0; i < nAhead; i++) {

            double ewma = predictionEWMA.get(i).getValue();

            DataPoint dataPoint = new DataPoint(ewma, lastTimestamp + (i * getCollectionInterval() * 1000));
            result.add(dataPoint);
            EngineLogger.LOGGER.tracef("Prediction: %s, %s", metric.getTenant(), metric.getId(), dataPoint);
        }

        return result;
    }

    private void addData(List<DataPoint> dataPoints) {
        // todo data from metrics arrives at <new>, <older>. Maybe its better to reverse itterate
        Collections.sort(dataPoints);

        Long newLastTimestamp = dataPoints.size() > 0 ?
                dataPoints.get(dataPoints.size() - 1).getTimestamp() : lastTimestamp;

        if (newLastTimestamp < lastTimestamp) {
            throw new IllegalArgumentException("Data point has older timestamp than current state.");
        }

        lastTimestamp = newLastTimestamp;

        ewma.learn(dataPoints);
        leastMeanSquaresFilter.learn(dataPoints);
    }

    @Override
    public Long getForecastingHorizon() {
        Long forecastingHorizon = null;

        if (modelOwners.contains(ModelManager.ModelOwner.Metric)) {
            forecastingHorizon = metric.getForecastingHorizon();
        } else if (modelOwners.contains(ModelManager.ModelOwner.MetricType)) {
            forecastingHorizon = metric.getMetricType().getForecastingHorizon();
        } else {
            forecastingHorizon = tenantSubscriptions.getForecastingHorizon();
        }

        return forecastingHorizon;
    }

    @Override
    public Long getCollectionInterval() {
        Long collectionInterval = null;

        if (metric.getCollectionInterval() != null) {
            collectionInterval =  metric.getCollectionInterval();
        } else if (metric.getMetricType() != null) {
            collectionInterval = metric.getMetricType().getCollectionInterval();
        }

        return collectionInterval;
    }

    public Set<ModelManager.ModelOwner> getModelOwners() {
        return new HashSet<>(modelOwners);
    }

    @Override
    public void addSubscriptionOwner(ModelManager.ModelOwner modelOwner) {
        this.modelOwners.add(modelOwner);
    }

    @Override
    public void addAllSubscriptionOwners(Set<ModelManager.ModelOwner> owners) {
        this.modelOwners.addAll(owners);
    }

    @Override
    public void removeSubscriptionOwner(ModelManager.ModelOwner modelOwner) {
        this.modelOwners.remove(modelOwner);
    }
}
