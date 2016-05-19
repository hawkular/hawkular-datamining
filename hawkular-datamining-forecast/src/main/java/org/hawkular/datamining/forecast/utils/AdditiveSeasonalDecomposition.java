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

package org.hawkular.datamining.forecast.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.models.SimpleMovingAverage;

/**
 * Additive time series decomposition
 *
 * @author Pavol Loffay
 */
public class AdditiveSeasonalDecomposition implements TimeSeriesDecomposition {

    private final int periods;
    private final List<DataPoint> original;

    private List<DataPoint> trend;
    private List<DataPoint> random;
    private List<DataPoint> seasonal;
    private double[] seasonalIndices;


    public AdditiveSeasonalDecomposition(List<DataPoint> points, int periods) {
        this.original = points;
        this.periods = periods;
    }

    /**
     *
     * @return Seasonal indices
     */
    public double[] decompose() {
        SimpleMovingAverage movingAverage = new SimpleMovingAverage(original, periods, true);
        trend = movingAverage.learn();

        // subtract trend from original (detrend)
        List<DataPoint> detrended = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) {
            Double trend = this.trend.get(i).getValue();
            Double value = trend != null ? original.get(i).getValue() - trend : null;
            detrended.add(new DataPoint(value, original.get(i).getTimestamp()));
        }

        /**
         * Seasonal
         * averages of each season of detrended series
         */
        int completeSeasons = original.size() / periods;
        completeSeasons += original.size() % 2 != 0 ? 1 : 0;
        seasonalIndices = new double[periods];
        for (int period = 0; period < periods; period++) {
            int seasonsWithoutNull = 0;
            for (int season = 0; season < completeSeasons && period + (periods*season) < original.size(); season++) {
                if (detrended.get(period + (periods*season)).getValue() == null) {
                    continue;
                }
                seasonalIndices[period] += detrended.get(period + (periods*season)).getValue();
                seasonsWithoutNull++;
            }
            seasonalIndices[period] = seasonalIndices[period] / (double) seasonsWithoutNull;
        }

        // subtract mean
        double mean = new Mean().evaluate(seasonalIndices);
        for (int i = 0 ; i < seasonalIndices.length; i++) {
            seasonalIndices[i] = seasonalIndices[i] - mean;
        }

        return Arrays.copyOf(seasonalIndices, seasonalIndices.length);
    }

    public List<DataPoint> seasonal() {
        if (seasonal == null && seasonalIndices != null) {
            seasonal = new ArrayList<>(original.size());
            for (int i = 0; i < original.size(); i++) {
                DataPoint point = new DataPoint(seasonalIndices[i % periods],original.get(i).getTimestamp());
                seasonal.add(point);
            }

        }
        return seasonal;
    }

    public List<DataPoint> random() {
        if (random == null && seasonalIndices != null) {
            List<DataPoint> seasonal = seasonal();

            random = new ArrayList<>(original.size());
            for (int i = 0; i < original.size(); i++) {
                Double value = trend.get(i).getValue() == null ? null :
                        original.get(i).getValue() - trend.get(i).getValue() - seasonal.get(i).getValue();
                random.add(new DataPoint(value, original.get(i).getTimestamp()));
            }
        }

        return random;
    }

    public List<DataPoint> trend() {
        return trend;
    }
}
