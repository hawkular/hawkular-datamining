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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.hawkular.datamining.forecast.utils.TimeSeriesDifferencing;
import org.hawkular.datamining.forecast.utils.TimeSeriesLag;

/**
 * Augmented Dickey-Fuller test
 * <p>
 * Null hypothesis H0: Time series contains unit root (is non stationary)
 *
 * <p>
 * Implemented variants:
 * <ul>
 *  <li> c - with constant (should be used for testing trend stationary)</li>
 *  <li> ct - with constant ant time trend </li>
 *  <li> nc - no constant </li>
 * </ul>
 *
 * <p>
 * Equations for ADF test
 * <ul>
 *   <li> yDiff = alpha + beta*t + gamma*yLag + sum(deltaLagT*yLagDiff) </li>
 *   <li> dfStatistics = gammaHat/standardError(gammaHat) </li>
 * </ul>
 *
 * <p>
 * Statistical tables adapted from:
 * <a href="https://github.com/statsmodels/statsmodels/blob/master/statsmodels/tsa/stattools.py">Python statsmodels</a>
 *
 * @author Pavol Loffay
 */
public class AugmentedDickeyFullerTest {

    /**
     * ADF test variants
     */
    public enum Type {
        /**
         * nc (no constant)
         */
        NoInterceptNoTimeTrend,
        /**
         * c (constant)
         */
        InterceptNoTimeTrend,
        /**
         * ct (constant trend)
         */
        InterceptTimeTrend
    }

    private final int maxLag;
    private final Type type;

    private Double adfStat;
    private Double pValue;


    /**
     * Default constructor for testing trend stationary time series. Type is set to InterceptNoTimeTrend (in R 'c' -
     * constant)
     * @param x
     * @param maxLag
     */
    public AugmentedDickeyFullerTest(double[] x, int maxLag) {
        this(x, maxLag, Type.InterceptNoTimeTrend);
    }

    public AugmentedDickeyFullerTest(double[] x, int maxLag, Type type) {
        this.maxLag = maxLag;
        this.type = type;

        calculateStatistics(x);
    }

    public Type getType() {
        return type;
    }

    public double pValue() {
        if (pValue == null) {
            pValue = macKinnonPValue(adfStat, 1);
        }

        return pValue;
    }

    public double statistics() {
        return adfStat;
    }

    private void calculateStatistics(double[] x) {
        double[] differences = TimeSeriesDifferencing.differencesAtLag(x, 1);

        double[] dependedVariable = Arrays.copyOfRange(differences, maxLag, differences.length);
        double[][] explanatoryVariables = explanatoryVariables(x, differences);

        // OLS model
        OLSMultipleLinearRegression olsMultipleLinearRegression = new OLSMultipleLinearRegression();
        olsMultipleLinearRegression.setNoIntercept(type == Type.NoInterceptNoTimeTrend);
        olsMultipleLinearRegression.newSampleData(dependedVariable, explanatoryVariables);

        double[] parameters = olsMultipleLinearRegression.estimateRegressionParameters();
        double[] standardErrors = olsMultipleLinearRegression.estimateRegressionParametersStandardErrors();

        // first parameter is intercept, second gamma*(lagged xt)
        int gammaIndex = (type == Type.NoInterceptNoTimeTrend) ? 0 : 1;
        adfStat = parameters[gammaIndex]/standardErrors[gammaIndex];
    }

    private double[][] explanatoryVariables(double[] x, double[] differences) {

        double[] xt = Arrays.copyOfRange(x, maxLag, differences.length);

        // xt + timeTrend + laggedDifferences
        int variablesSize = (type == Type.InterceptTimeTrend ? 2 : 1) + maxLag;
        double[][] explanatory = new double[variablesSize][];

        explanatory[0] = xt;
        if (type == Type.InterceptTimeTrend) {
            explanatory[1] = timeTrend(maxLag + 1, differences.length);
        }

        double[][] laggedDifferences = TimeSeriesLag.lag(differences, maxLag + 1);
        for (int i = 1; i < laggedDifferences.length; i++) {
            explanatory[variablesSize - maxLag + (i - 1)] =
                    Arrays.copyOf(laggedDifferences[i], laggedDifferences[i].length);
        }

        return transpose(explanatory);
    }

    private double[] timeTrend(int start, int length) {
        double[] result = new double[length - start + 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = i + start;
        }

        return result;
    }

    private double[][] transpose(double[][] arr) {
        double[][] result = new double[arr[0].length][arr.length];

        for (int row = 0; row < arr.length; row++) {
            for (int column = 0; column < arr[0].length; column++) {
                result[column][row] = arr[row][column];
            }
        }

        return result;
    }


    /**
     * Returns MacKinnon's approximate p-value for the given test statistic.
     *
     * MacKinnon, J.G. 1994  "Approximate Asymptotic Distribution Functions for
     *    Unit-Root and Cointegration Tests." Journal of Business & Economics
     *    Statistics, 12.2, 167-76.
     *
     * @param testStat "T-value" from an Augmented Dickey-Fuller regression.
     * @param maxLag The number of series believed to be I(1). For (Augmented) Dickey-Fuller n = 1.
     * @return The p-value for the ADF statistic using MacKinnon 1994.
     */
    private double macKinnonPValue(double testStat, int maxLag) {
        double[] maxStat = ADF_TAU_MAX.get(type);
        if (testStat > maxStat[maxLag - 1]) {
            return 1.0;
        }
        double[] minStat = ADF_TAU_MIN.get(type);
        if (testStat < minStat[maxLag - 1]) {
            return 0.0;
        }

        double[] starStat = ADF_TAU_STAR.get(type);
        double[] tauCoef = testStat <= starStat[maxLag - 1] ?
                ADF_TAU_SMALLP.get(type)[maxLag - 1] :
                ADF_TAU_LARGEP.get(type)[maxLag - 1];

        return new NormalDistribution().cumulativeProbability(polyval(tauCoef, testStat));
    }

    private double polyval(double[] coefs, double x) {
        double result = 0;

        for (int i = coefs.length - 1; i >= 0; i--) {
            result = result*x + coefs[i];
        }

        return result;
    }

    private static final Map<Type, double[]> ADF_TAU_MAX = new EnumMap<>(Type.class);
    private static final Map<Type, double[]> ADF_TAU_MIN = new EnumMap<>(Type.class);
    private static final Map<Type, double[]> ADF_TAU_STAR = new EnumMap<>(Type.class);
    private static final Map<Type, double[][]> ADF_TAU_SMALLP = new EnumMap<>(Type.class);
    private static final Map<Type, double[][]> ADF_TAU_LARGEP = new EnumMap<>(Type.class);
    private static final double[] ADF_LARGE_SCALING = {1.0, 1e-1, 1e-1, 1e-2};
    static {
        ADF_TAU_MAX.put(Type.NoInterceptNoTimeTrend, new double[] {1.51, 0.86, 0.88, 1.05, 1.24});
        ADF_TAU_MAX.put(Type.InterceptNoTimeTrend, new double[] {2.74, 0.92, 0.55, 0.61, 0.79, 1});
        ADF_TAU_MAX.put(Type.InterceptTimeTrend, new double[] {0.7, 0.63, 0.71, 0.93, 1.19, 1.42});

        ADF_TAU_MIN.put(Type.NoInterceptNoTimeTrend, new double[]{-19.04, -19.62, -21.21, -23.25, -21.63, -25.74});
        ADF_TAU_MIN.put(Type.InterceptNoTimeTrend, new double[]{-18.83, -18.86, -23.48, -28.07, -25.96, -23.27});
        ADF_TAU_MIN.put(Type.InterceptTimeTrend, new double[]{-16.18, -21.15, -25.37, -26.63, -26.53, -26.18});

        ADF_TAU_STAR.put(Type.NoInterceptNoTimeTrend, new double[]{-1.04, -1.53, -2.68, -3.09, -3.07, -3.77});
        ADF_TAU_STAR.put(Type.InterceptNoTimeTrend, new double[]{-1.61, -2.62, -3.13, -3.47, -3.78, -3.93});
        ADF_TAU_STAR.put(Type.InterceptTimeTrend, new double[]{-2.89, -3.19, -3.50, -3.65, -3.80, -4.36});

        ADF_TAU_SMALLP.put(Type.NoInterceptNoTimeTrend, new double[][]{
                new double[]{0.6344, 1.2378, 3.2496 * 1e-2},
                new double[]{1.9129, 1.3857, 3.5322 * 1e-2},
                new double[]{2.7648, 1.4502, 3.4186 * 1e-2},
                new double[]{3.4336, 1.4835, 3.19 * 1e-2},
                new double[]{4.0999, 1.5533, 3.59 * 1e-2},
                new double[]{4.5388, 1.5344, 2.9807 * 1e-2}
        });
        ADF_TAU_SMALLP.put(Type.InterceptNoTimeTrend, new double[][]{
                new double[]{2.1659, 1.4412, 3.8269 * 1e-2},
                new double[]{2.92, 1.5012, 3.9796 * 1e-2},
                new double[]{3.4699, 1.4856, 3.164 * 1e-2},
                new double[]{3.9673, 1.4777, 2.6315 * 1e-2},
                new double[]{4.5509, 1.5338, 2.9545 * 1e-2},
                new double[]{5.1399, 1.6036, 3.4445 * 1e-2}
        });
        ADF_TAU_SMALLP.put(Type.InterceptTimeTrend, new double[][]{
                new double[]{3.2512, 1.6047, 4.9588 * 1e-2},
                new double[]{3.6646, 1.5419, 3.6448 * 1e-2},
                new double[]{4.0983, 1.5173, 2.9898 * 1e-2},
                new double[]{4.5844, 1.5338, 2.8796 * 1e-2},
                new double[]{5.0722, 1.5634, 2.9472 * 1e-2},
                new double[]{5.53, 1.5914, 3.0392 * 1e-2}
        });

        ADF_TAU_LARGEP.put(Type.NoInterceptNoTimeTrend, new double[][]{
                new double[]{0.4797, 9.3557, -0.6999, 3.3066},
                new double[]{1.5578, 8.558, -2.083, -3.3549},
                new double[]{2.2268, 6.8093, -3.2362, -5.4448},
                new double[]{2.7654, 6.4502, -3.0811, -4.4946},
                new double[]{3.2684, 6.8051, -2.6778, -3.4972},
                new double[]{3.7268, 7.167, -2.3648, -2.8288},
        });
        ADF_TAU_LARGEP.put(Type.InterceptNoTimeTrend, new double[][]{
                new double[]{1.7339, 9.3202, -1.2745, -1.0368},
                new double[]{2.1945, 6.4695, -2.9198, -4.2377},
                new double[]{2.5893, 4.5168, -3.6529, -5.0074},
                new double[]{3.0387, 4.5452, -3.3666, -4.1921},
                new double[]{3.5049, 5.2098, -2.9158, -3.3468},
                new double[]{3.9489, 5.8933, -2.5359, -2.721},
        });
        ADF_TAU_LARGEP.put(Type.InterceptTimeTrend, new double[][]{
                new double[]{2.5261, 6.1654, -3.7956, -6.0285},
                new double[]{2.85, 5.272, -3.6622, -5.1695},
                new double[]{3.221, 5.255, -3.2685, -4.1501},
                new double[]{3.652, 5.9758, -2.7483, -3.2081},
                new double[]{4.0712, 6.6428, -2.3464, -2.546},
                new double[]{4.4735, 7.1757, -2.0681, -2.1196},
        });

        ADF_TAU_LARGEP.entrySet().stream().forEach(typeEntry -> {
            double[][] values = typeEntry.getValue();
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[i].length; j++) {
                    values[i][j] = values[i][j]*ADF_LARGE_SCALING[j];
                }
            }
        });
    }
}
