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

package org.hawkular.datamining.forecast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hawkular.datamining.forecast.models.DoubleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.ModelOptimizer;
import org.hawkular.datamining.forecast.models.SimpleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.TimeSeriesModel;
import org.hawkular.datamining.forecast.models.TripleExponentialSmoothing;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.hawkular.datamining.forecast.stats.InformationCriterion;
import org.hawkular.datamining.forecast.stats.InformationCriterionHolder;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class AutomaticForecaster implements Forecaster {

    // recalculation period
    public static final int WINDOW_SIZE = 50;

    private long counter;
    private long lastTimestamp; // in ms
    private final EvictingQueue<DataPoint> window;

    private TimeSeriesModel usedModel;
    private final Set<ModelOptimizer> applicableModels;

    private final MetricContext metricContext;

    private final InformationCriterion ic = InformationCriterion.AICc;

    public AutomaticForecaster(MetricContext context) {
        if (context == null ||
                context.getCollectionInterval() == null || context.getCollectionInterval() <= 0) {
            throw new IllegalArgumentException("Invalid context.");
        }

        this.applicableModels = new HashSet<>(Arrays.asList(
                SimpleExponentialSmoothing.optimizer(),
                DoubleExponentialSmoothing.optimizer(),
                TripleExponentialSmoothing.optimizer()));
        this.window = EvictingQueue.create(WINDOW_SIZE);
        this.metricContext = context;
    }

    @Override
    public void learn(DataPoint dataPoint) {
        learn(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {

        sortPoints(dataPoints);

        // recalculate if model is null or periodically after X points
        if ((counter + dataPoints.size()) / WINDOW_SIZE > 0 || usedModel == null) {
            selectBestModel(dataPoints);
        } else if (usedModel != null) {
            usedModel.learn(dataPoints);
        }

        counter += dataPoints.size();
        if (counter / WINDOW_SIZE > 0) {
            counter = (counter + dataPoints.size()) % WINDOW_SIZE;
        }

        window.addAll(dataPoints);
    }

    @Override
    public DataPoint forecast() {
        if (!initialized()) { //todo may be do not throw exception
            throw new IllegalStateException("Model not initialized, window remaining capacity = " +
                    window.remainingCapacity());
        }
        DataPoint point = usedModel.forecast();
        return new DataPoint(point.getValue(), lastTimestamp + metricContext.getCollectionInterval()*1000);
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        if (!initialized()) {
            throw new IllegalStateException("Model not initialized, window remaining capacity = " +
                    window.remainingCapacity());
        }
        List<DataPoint> points = usedModel.forecast(nAhead);

        return points.stream().map(dataPoint -> new DataPoint(dataPoint.getValue(),
                lastTimestamp + dataPoint.getTimestamp()*metricContext.getCollectionInterval()*1000))
                .collect(Collectors.toList());
    }

    @Override
    public TimeSeriesModel model() {
        return usedModel;
    }

    @Override
    public MetricContext context() {
        return metricContext;
    }

    @Override
    public boolean initialized() {
        return usedModel != null;
    }

    private TimeSeriesModel selectBestModel(List<DataPoint> dataPoints) {
        final List<DataPoint> initPoints = new ArrayList<>();
        initPoints.addAll(window);
        initPoints.addAll(dataPoints);

        if (initPoints.isEmpty()) {
            return null;
        }

        Logger.LOGGER.debugf("Estimating best model for: %s, previous: %s", metricContext.getMetricId(), usedModel);

        TimeSeriesModel bestModel = null;
        double bestIC = Double.POSITIVE_INFINITY;

        for (ModelOptimizer modelOptimizer : applicableModels) {

            TimeSeriesModel model = null;
            try {
                model = modelOptimizer.minimizedMSE(initPoints);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            AccuracyStatistics initStatistics = model.initStatistics();

            InformationCriterionHolder icHolder = new InformationCriterionHolder(initStatistics.getSse(),
                    model.numberOfParams(), initPoints.size());

            Logger.LOGGER.debugf("Estimated model: %s, data size: %d,init MSE: %f, %s",
                    model.toString(), initPoints.size(), initStatistics.getMse(), icHolder);

            double currentIc = icHolder.informationCriterion(ic);
            if (currentIc < bestIC) {
                bestIC = currentIc;
                bestModel = model;
            }
        }

        Logger.LOGGER.debugf("Best model for: %s, is %s, %s", metricContext.getMetricId(),
                bestModel.getClass().getSimpleName(), bestModel.initStatistics());

        this.usedModel = bestModel;
        return bestModel;
    }

    private List<DataPoint> sortPoints(List<DataPoint> dataPoints) {
        Collections.sort(dataPoints);

        Long newLastTimestamp = dataPoints.size() > 0 ?
                dataPoints.get(dataPoints.size() - 1).getTimestamp() : lastTimestamp;

        if (newLastTimestamp < lastTimestamp) {
            throw new IllegalArgumentException("Data point has older timestamp than current state.");
        }

        lastTimestamp = newLastTimestamp;

        return dataPoints;
    }
}
