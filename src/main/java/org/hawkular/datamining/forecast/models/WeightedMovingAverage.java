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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;

import com.google.common.collect.EvictingQueue;

/**
 * Weighted moving average model, variant of Moving average model. In difference to simple
 * moving averages different weight can be assigned for each point in the window.
 *
 * In R this model is implemented in filter() function from stats package.
 *
 * @author Pavol Loffay
 */
public class WeightedMovingAverage {

    private final double[] weights;
    private List<DataPoint> dataPoints;


    public WeightedMovingAverage(List<DataPoint> dataPoints, double[] weights) {
        if (dataPoints.size() < weights.length) {
            throw new IllegalArgumentException("More weights than data points");
        }

        this.weights = Arrays.copyOf(weights, weights.length);
        this.dataPoints = dataPoints;
    }

    public List<DataPoint> learn() {

        List<DataPoint> result = new ArrayList<>(dataPoints.size());
        EvictingQueue<Double> window = EvictingQueue.create(weights.length);

        int endHalf = weights.length / 2;

        // add zeros to the beginning
        for (int i = 0; i < (weights.length - endHalf) - 1; i++) {
            result.add(new DataPoint(null, dataPoints.get(i).getTimestamp()));
        }

        for (int i = 0; i < dataPoints.size(); i++) {
            window.add(dataPoints.get(i).getValue());

            if (window.remainingCapacity() == 0) {
                Iterator<Double> iterator = window.iterator();
                int counter = 0;
                double sum = 0;
                while (iterator.hasNext()) {
                    double value = iterator.next();
                    sum += value*weights[counter++];
                }

                result.add(new DataPoint(sum, dataPoints.get(i - endHalf).getTimestamp()));
            }
        }

        // add zeros to end
        for (int i = result.size(); i < dataPoints.size(); i++) {
            result.add(new DataPoint(null, dataPoints.get(i).getTimestamp()));
        }

        return result;
    }
}
