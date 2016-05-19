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

import java.util.Arrays;

/**
 * Time series differencing
 *
 * <p>
 * first order difference: y[t] = y[t] - y[t-1]
 *
 * <p>
 * In R is this function implemented in diff function
 *
 * @author Pavol Loffay
 */
public class TimeSeriesDifferencing {

    private TimeSeriesDifferencing() {
    }

    /**
     * @param x time series
     * @param differences order of differences
     * @return differenced time series
     */
    public static double[] differencesAtLag(double[] x, int differences) {

        if (differences < 1) {
            throw new IllegalArgumentException("Differences should be > 0");
        }
        if (x == null || x.length <= differences) {
            throw new IllegalArgumentException("Not enough values for differencing");
        }

        double[] temp = Arrays.copyOf(x, x.length);

        for (int i = 0; i < differences; i++) {
            firstDifferences(temp);
        }

        return Arrays.copyOfRange(temp, 0, x.length - differences);
    }

    private static void firstDifferences(double[] x)  {

        for (int i = 1; i < x.length; i++) {
            x[i - 1] = x[i] - x[i - 1];
        }
    }
}
