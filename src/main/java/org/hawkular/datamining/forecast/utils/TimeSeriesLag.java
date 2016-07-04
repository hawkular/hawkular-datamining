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

/**
 * Compute a lagged version of a time series
 *
 * @author Pavol Loffay
 */
public class TimeSeriesLag {

    private TimeSeriesLag() {
    }

    public static double[][] lag(double[] x, int maxLag) {

        if (maxLag < 1) {
            throw new IllegalArgumentException("maxLag should be greater than 0");
        }
        if (x == null || maxLag > x.length) {
            throw new IllegalArgumentException("The length of array should be greater than maxLag");
        }

        double[][] result = new double[maxLag][];

        for (int lag = 1; lag <= maxLag; lag++) {
            double[] arr = new double[x.length - (maxLag - 1)];

            for (int i = 0; i < x.length - (maxLag - 1); i++) {
                arr[i] = x[(maxLag - lag) + i];
            }

            result[lag - 1] = arr;
        }

        return result;
    }
}
