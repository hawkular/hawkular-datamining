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
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.stats.AugmentedDickeyFullerTest;
import org.hawkular.datamining.forecast.stats.AutoCorrelationFunction;

/**
 * Finds number of period of time series. Should work for arbitrary time series.
 *
 * Algorithm:
 * 1. If a time series is trend stationary trend is removed using first differences
 * 2. Computes auto correlation function for the length of the series
 * 3. Finds highest correlation of the series and set period its index
 * 4. Checks if significant(@see +-CORRELATION_MIN/MAX_CHANGE) correlations exists at following periods
 *  e.g. found period was 4, so it checks if at index 8, 16, 20 exists significant correlations
 * 5. If failed go to step 3.
 *
 * @author Pavol Loffay
 */
public class AutomaticPeriodIdentification {

    public static final double ADF_TEST_DEFAULT_CRITICAL_PVALUE = 0.05;

    private static final double CORRELATION_MIN_CHANGE = 0.50;
    private static final double CORRELATION_MAX_CHANGE = 1.50;

    private static final int NO_PERIOD = 1;


    private AutomaticPeriodIdentification() {
    }

    public static int periods(final List<DataPoint> data) {
        return periods(data, ADF_TEST_DEFAULT_CRITICAL_PVALUE);
    }

    public static int periods(final List<DataPoint> data, double criticalValue) {
        double[] x = Utils.toArray(data);

        if (trendStationary(x, criticalValue)) {
            x = TimeSeriesDifferencing.differencesAtLag(x, 1);
        }

        double[] acf = AutoCorrelationFunction.correlation(x, x.length - 1);

        // skip index 0 (acf[0] = 1)
        int period = 4;

        while (period*2 < data.size()) {
            period = findHighest(acf, period);

            if (period*2 > data.size()) {
                return NO_PERIOD;
            }

            double[] acfToValidatePeriod = Arrays.copyOfRange(acf, 0, acfLengthForValidation(acf.length, period));
            if (checkPeriodExists(acfToValidatePeriod, period)) {
                return period;
            }

            period++;
        }

        return NO_PERIOD;
    }

    private static int findHighest(double[] acf, int from) {

        int period = from;
        double max = Double.MIN_VALUE;

        for (int i = from; i < acf.length; i++) {

            if (acf[i] > max) {
                period = i;
                max = acf[i];
            }
        }

        return period;
    }

    private static int acfLengthForValidation(int acfLength, int period) {

        if (3*period*acfLength <= acfLength) {
            return 3*period;
        }

        return 2*period;
    }

    /**
     * Checks if a specific period is present
     * @param acf autocorrelation function, at least two seasons of given period
     * @param period
     * @return true if the period is present
     */
    private static boolean checkPeriodExists(double[] acf, int period) {

        if (period*2 < acf.length) {
            return false;
        }

        double correlation = acf[period];

        // significant correlation should be present at each period
        for (int i = period; i < acf.length; i += period) {

            if (acf[i] < correlation* CORRELATION_MIN_CHANGE || acf[i] > correlation* CORRELATION_MAX_CHANGE) {
                return false;
            } else  {
                correlation = acf[i];
            }
        }

        return true;
    }

    private static boolean trendStationary(double[] x, double criticalValue) {

        AugmentedDickeyFullerTest adfTest = new AugmentedDickeyFullerTest(x, 1);
        double pValue = adfTest.pValue();

        return pValue > criticalValue;
    }
}
