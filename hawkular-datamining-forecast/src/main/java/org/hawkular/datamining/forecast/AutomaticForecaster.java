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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hawkular.datamining.forecast.model.DoubleExponentialSmoothing;
import org.hawkular.datamining.forecast.model.ModelOptimization;
import org.hawkular.datamining.forecast.model.SimpleExponentialSmoothing;
import org.hawkular.datamining.forecast.model.TimeSeriesModel;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class AutomaticForecaster implements Forecaster {

    public static final int WINDOW_SIZE = 10;
    public static final int MIN_SIZE = 20;

    private long counter;
    private long lastTimestamp; // in ms
    private final EvictingQueue<DataPoint> window;

    private TimeSeriesModel usedModel;
    private final Set<ModelOptimization> applicableModels;

    private final MetricContext metricContext;

    public AutomaticForecaster(MetricContext context) {
        if (context == null ||
                context.getCollectionInterval() == null || context.getCollectionInterval() <= 0) {
            throw new IllegalArgumentException("Invalid context.");
        }

        this.applicableModels = new HashSet<>(Arrays.asList(
                new SimpleExponentialSmoothing.Optimizer(),
                new DoubleExponentialSmoothing.Optimizer()));
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

        dataPoints.forEach(dataPoint -> {
            window.add(dataPoint);

            if (++counter % WINDOW_SIZE == 0) {
                useBestModel();
                counter = 0;
            }

            if (usedModel != null) {
                usedModel.learn(dataPoint);
            }
        });
    }

    @Override
    public DataPoint forecast() {
        if (!initialized()) {
            throw new IllegalStateException("Model not initialized, window remaining capacity = " +
                    window.remainingCapacity());
        }
        DataPoint point = usedModel.forecast();
        return new DataPoint(point.getValue(), lastTimestamp + metricContext.getCollectionInterval());
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        if (!initialized()) {
            throw new IllegalStateException("Model not initialized, window remaining capacity = " +
                    window.remainingCapacity());
        }
        List<DataPoint> points = usedModel.forecast(nAhead);

        return points.stream().map(dataPoint -> new DataPoint(dataPoint.getValue(),
                lastTimestamp + dataPoint.getTimestamp() * metricContext.getCollectionInterval()))
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

    private TimeSeriesModel useBestModel() {

        TimeSeriesModel bestModel = null;
        double bestModelMSE = Double.MAX_VALUE;

        for (ModelOptimization modelOptimizer : applicableModels) {
            final List<DataPoint> initPoints = Arrays.asList(window.toArray(new DataPoint[0]));

            TimeSeriesModel model = modelOptimizer.minimizedMSE(initPoints);

            AccuracyStatistics accuracy = model.init(initPoints);

            if (accuracy.getMse() < bestModelMSE) {
                bestModelMSE = accuracy.getMse();
                bestModel = model;
            }
        }

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
