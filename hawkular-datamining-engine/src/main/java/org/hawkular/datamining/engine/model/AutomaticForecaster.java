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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.datamining.api.AccuracyStatistics;
import org.hawkular.datamining.api.Forecaster;
import org.hawkular.datamining.api.ModelOptimization;
import org.hawkular.datamining.api.TimeSeriesModel;
import org.hawkular.datamining.api.model.DataPoint;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class AutomaticForecaster implements Forecaster {

    public static final int WINDOW_SIZE = 50;
    public static final int MIN_SIZE = 20;

    private long counter;
    private long lastTimestamp; // in ms
    private final EvictingQueue<DataPoint> window;

    private TimeSeriesModel usedModel;
    private final Set<ModelOptimization> applicableModels;


    public AutomaticForecaster() {
        applicableModels = new HashSet<>(Arrays.asList(
                new SimpleExponentialSmoothing.Optimizer(),
                new DoubleExponentialSmoothing.Optimizer()));

        window = EvictingQueue.create(WINDOW_SIZE);
    }

    @Override
    public void learn(DataPoint dataPoint) {
        learn(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        sortPoints(dataPoints);
        window.addAll(dataPoints);

        counter += dataPoints.size();

        if (dataPoints.size() >= WINDOW_SIZE || counter % WINDOW_SIZE == 0) {
            useBestModel();
        }

        usedModel.learn(dataPoints);
    }

    @Override
    public DataPoint forecast() {
        return usedModel.forecast();
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        return usedModel.forecast(nAhead);
    }

    @Override
    public TimeSeriesModel model() {
        return usedModel;
    }

    private TimeSeriesModel useBestModel() {

        TimeSeriesModel bestModel = null;
        double bestModelMSE = Double.MAX_VALUE;

        for (ModelOptimization modelOptimizer : applicableModels) {
            final List<DataPoint> initPoints = Arrays.asList(window.toArray(new DataPoint[0]));

            TimeSeriesModel model = modelOptimizer.minimizedMSE(initPoints);

            AccuracyStatistics accurancy = model.init(initPoints);

            if (accurancy.getMse() < bestModelMSE) {
                bestModelMSE = accurancy.getMse();
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
