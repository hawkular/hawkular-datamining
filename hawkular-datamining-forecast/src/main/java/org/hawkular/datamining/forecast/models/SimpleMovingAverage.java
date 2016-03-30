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

package org.hawkular.datamining.forecast.models;

import java.util.Arrays;
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;

/**
 * Simple moving average model, often used for trend estimating
 *
 * @author Pavol Loffay
 */
public class SimpleMovingAverage {

    private final List<DataPoint> dataPoints;
    private final int smoothingLength;
    private final boolean symmetric;


    public SimpleMovingAverage(List<DataPoint> dataPoints, int smoothingLength ,boolean symmetric) {
        if (smoothingLength < 1) {
            throw new IllegalArgumentException("Smoothing length has to be > 0");
        }

        this.dataPoints = dataPoints;
        this.smoothingLength = smoothingLength;
        this.symmetric = symmetric;
    }

    public List<DataPoint> learn() {

        if (dataPoints.size() < smoothingLength) {
            throw new IllegalArgumentException("Not enough data!, minimum size = " + smoothingLength);
        }

        double[] weights;
        if (symmetric && smoothingLength % 2 == 0) {
            weights = new double[smoothingLength - 1 + 2]; // - center + (2xMA)
            Arrays.fill(weights, 1, weights.length - 1, 1.0/smoothingLength );
            weights[0] = 0.5/smoothingLength;
            weights[weights.length - 1] = 0.5/smoothingLength;
        } else {
            weights = new double[smoothingLength];
            Arrays.fill(weights, 1.0/smoothingLength);
        }

        WeightedMovingAverage weightedMovingAverage = new WeightedMovingAverage(dataPoints, weights);
        return weightedMovingAverage.learn();
    }
}
