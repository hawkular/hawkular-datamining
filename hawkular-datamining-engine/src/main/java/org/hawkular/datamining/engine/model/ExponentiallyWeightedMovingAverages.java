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

package org.hawkular.datamining.engine.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hawkular.datamining.api.model.DataPoint;

/**
 * @author Pavol Loffay
 */
public class ExponentiallyWeightedMovingAverages implements PredictionModel {

    private double alpha;
    private double beta;

    private double level;
    private double slope;


    public ExponentiallyWeightedMovingAverages(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        process(Arrays.asList(dataPoint));
    }

    @Override
    public void addDataPoints(List<DataPoint> dataPoints) {
        process(dataPoints);
    }

    @Override
    public List<DataPoint> predict(int nAhead) {

        double value = level + nAhead * slope;
        DataPoint dataPoint = new DataPoint(value, null);
        return Arrays.asList(dataPoint);
    }

    private void process(Collection<DataPoint> dataPoints) {

        dataPoints.forEach(point -> {
            double level_old = level;
            level = alpha * point.getValue() + (1 - alpha) * (level + slope);
            slope = beta * (level - level_old) + (1 - beta) * (slope);
        });
    }
}
