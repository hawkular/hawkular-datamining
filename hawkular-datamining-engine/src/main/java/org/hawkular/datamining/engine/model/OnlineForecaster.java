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
import org.hawkular.datamining.api.TimeSeriesModel;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.engine.EngineLogger;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class OnlineForecaster implements TimeSeriesLinkedModel {

    private final Metric metric;
    private final TenantSubscriptions tenantSubscriptions;
    private Set<ModelManager.ModelOwner> modelOwners = new HashSet<>();

    private TimeSeriesModel usedModel;

    // in ms
    private long lastTimestamp;

    private EvictingQueue<DataPoint> oldPoints;


    public OnlineForecaster(Metric metric, TenantSubscriptions tenantSubscriptions,
                            Set<ModelManager.ModelOwner> modelOwner) {
        this.metric = metric;
        this.tenantSubscriptions = tenantSubscriptions;
        this.modelOwners.addAll(modelOwner);
    }

    @Override
    public Metric getLinkedMetric() {
        return metric;
    }

    @Override
    public void learn(DataPoint dataPoint) {
        learn(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        // todo data from metrics arrives at <new>, <older>. Maybe its better to reverse iterate
        Collections.sort(dataPoints);

        Long newLastTimestamp = dataPoints.size() > 0 ?
                dataPoints.get(dataPoints.size() - 1).getTimestamp() : lastTimestamp;

        if (newLastTimestamp < lastTimestamp) {
            throw new IllegalArgumentException("Data point has older timestamp than current state.");
        }

        lastTimestamp = newLastTimestamp;
        oldPoints.addAll(dataPoints);

        usedModel.learn(dataPoints);
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
        List<DataPoint> prediction = usedModel.predict(nAhead);

        for (int i = 0; i < nAhead; i++) {

            double ewma = prediction.get(i).getValue();

            DataPoint dataPoint = new DataPoint(ewma, lastTimestamp + (i * getCollectionInterval() * 1000));
            result.add(dataPoint);
            EngineLogger.LOGGER.tracef("Prediction: %s, %s", metric.getTenant(), metric.getId(), dataPoint);
        }

        return result;
    }

    @Override
    public double mse() {
        return usedModel.mse();
    }

    @Override
    public double mae() {
        return usedModel.mae();
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

    @Override
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
