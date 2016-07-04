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

package org.hawkular.datamining.forecast.stats;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

/**
 * ACF - autocorrelation function
 *
 * @author Pavol Loffay
 */
public class AutoCorrelationFunction {

    private AutoCorrelationFunction() {
    }

    /**
     * Autocorrelation function AFC
     * complexity: O(n^2), could be optimized to n*log(n) using FFT
     * http://stackoverflow.com/questions/12239096/computing-autocorrelation-with-fft-using-jtransforms-library
     * http://dsp.stackexchange.com/questions/3337/finding-peaks-in-an-autocorrelation-function
     * in python
     * http://stats.stackexchange.com/questions/85686/how-to-test-for-presence-of-trend-in-time-series
     *
     * @return array of autocorrelations including lag 0, size is maxLag + 1
     */
    public static double[] correlation(double[] x, int maxLag) {
        return evaluate(x, maxLag, true);
    }

    public static double[] covariance(double[] x, int maxLag) {
        return evaluate(x, maxLag, false);
    }

    private static double[] evaluate(final double[] x, int maxLag, boolean correlation) {
        // max lag = length - 1
        if (maxLag >= x.length) {
            throw new IllegalArgumentException("Lag is higher than ");
        }

        double mean = new Mean().evaluate(x);
        double var = correlation ? new Variance().evaluate(x, mean) : 1;
        int lengthForMean = correlation ? x.length - 1 : x.length;

        double[] acf = new double[maxLag + 1];
        for (int lag = 0; lag < maxLag + 1; lag++) {

            double sum = 0;
            for (int i = 0; i + lag < x.length; i++) {

                sum += (x[i] - mean)*(x[i + lag] - mean);
            }

            sum /= lengthForMean;
            acf[lag] = sum / var;
        }

        return acf;
    }
}
