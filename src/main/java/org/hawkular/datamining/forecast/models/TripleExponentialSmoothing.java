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

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.hawkular.datamining.forecast.MetricContext;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.hawkular.datamining.forecast.utils.AdditiveSeasonalDecomposition;
import org.hawkular.datamining.forecast.utils.AutomaticPeriodIdentification;
import org.hawkular.datamining.forecast.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.EvictingQueue;

/**
 * Triple exponential smoothing model also known as Holt-Winters model. This model implements additive variant.
 *
 * @author Pavol Loffay
 */
public class TripleExponentialSmoothing extends AbstractExponentialSmoothing {
    
    private static final Logger log = LoggerFactory.getLogger(TripleExponentialSmoothing.class);

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;
    public static final double DEFAULT_SEASONAL_SMOOTHING = 0.1;

    public static final double MIN_LEVEL_SMOOTHING = 0.0001;
    public static final double MIN_TREND_SMOOTHING = 0.0001;
    public static final double MIN_SEASONAL_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_SMOOTHING = 0.9999;
    public static final double MAX_TREND_SMOOTHING = 0.9999;
    public static final double MAX_SEASONAL_SMOOTHING = 0.9999;

    private TripleExState state;
    private final double levelSmoothing;
    private final double trendSmoothing;
    private final double seasonalSmoothing;

    private final int periods;

    private EvictingQueue<Double>[] residuals;

    public static class TripleExState extends DoubleExponentialSmoothing.DoubleExState {
        protected double[] periods;
        protected long firstTimestamp;

        public TripleExState(double level, double slope, double[] periods, long firstTimestamp) {
            super(level, slope);
            this.periods = Arrays.copyOf(periods, periods.length);
            this.firstTimestamp = firstTimestamp;
        }
    }


    @SuppressWarnings("unchecked")
    private TripleExponentialSmoothing(int periods, double levelSmoothing, double trendSmoothing,
                                      double seasonalSmoothing, MetricContext metricContext) {
        super(metricContext);

        if (levelSmoothing < MIN_LEVEL_SMOOTHING || levelSmoothing > MAX_LEVEL_SMOOTHING) {
            throw new IllegalArgumentException("Level smoothing should be in interval 0-1");
        }
        if (trendSmoothing < MIN_TREND_SMOOTHING || trendSmoothing > MAX_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Trend smoothing should be in 0-1");
        }
        if (seasonalSmoothing < MIN_SEASONAL_SMOOTHING || seasonalSmoothing > MAX_SEASONAL_SMOOTHING) {
            throw new IllegalArgumentException("Seasonal smoothing should be in 0-1");
        }

        if (periods < 2) {
            throw new IllegalArgumentException("Periods < 2, use non seasonal model.");
        }

        this.periods = periods;
        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
        this.seasonalSmoothing = seasonalSmoothing;
        this.residuals = new EvictingQueue[this.periods];
        for (int i = 0; i < residuals.length; i++) {
            this.residuals[i] = EvictingQueue.create(50);
        }
    }

    public static TripleExponentialSmoothing createDefault(int periods) {
        return new TripleExponentialSmoothing(periods, DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING,
                DEFAULT_SEASONAL_SMOOTHING, ImmutableMetricContext.getDefault());
    }

    public static TripleExponentialSmoothing createWithMetric(int periods, MetricContext metricContext) {
        return new TripleExponentialSmoothing(periods, DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING,
                DEFAULT_SEASONAL_SMOOTHING, metricContext);
    }

    public static TripleExponentialSmoothing createWithSmoothingParams(int periods, double levelSmoothing,
                                                                       double trendSmoothing,
                                                                       double seasonalSmoothing) {
        return new TripleExponentialSmoothing(periods, levelSmoothing, trendSmoothing, seasonalSmoothing,
                ImmutableMetricContext.getDefault());
    }

    public static TripleExponentialSmoothing createCustom(int periods, double levelSmoothing,
                                                          double trendSmoothing,
                                                          double seasonalSmoothing,
                                                          MetricContext metricContext) {
        return new TripleExponentialSmoothing(periods, levelSmoothing, trendSmoothing, seasonalSmoothing,
                metricContext);
    }

    public static TripleExponentialSmoothing createWithState(TripleExState state, double levelSmoothing, double trendSmoothing,
                                                             double seasonalSmoothing, MetricContext metricContext) {
        TripleExponentialSmoothing tripleExponentialSmoothing =
                new TripleExponentialSmoothing(state.periods.length, levelSmoothing, trendSmoothing,
                        seasonalSmoothing, metricContext);
        tripleExponentialSmoothing.state = state;

        return tripleExponentialSmoothing;
    }

    @Override
    public String name() {
        return "Triple exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 5 + periods;
    }

    @Override
    public int minimumInitSize() {
        return periods*2;
    }

    public static TripleExState initState(List<DataPoint> dataPoints, int periods, MetricContext metricContext) {
        return TripleExponentialSmoothing.createWithMetric(periods, metricContext).initState(dataPoints);
    }

    @Override
    protected TripleExState initState(List<DataPoint> dataPoints) {

        if (dataPoints.size() < minimumInitSize()) {
            throw new IllegalArgumentException("At least two complete seasons are required");
        }

        AdditiveSeasonalDecomposition decomposition = new AdditiveSeasonalDecomposition(dataPoints, periods);
        double[] periodIndices = decomposition.decompose();

        // do regression on seasonally adjusted data points
        List<DataPoint> seasonal = decomposition.seasonal();
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < dataPoints.size(); i++) {
            regression.addData(i, dataPoints.get(i).getValue() - seasonal.get(i).getValue());
        }
        double level = regression.predict(0);
        double slope = regression.getSlope();

        double[] switchedPeriods = rotatePeriods(periodIndices, 0);

        return state = new TripleExState(level, slope, switchedPeriods, dataPoints.get(0).getTimestamp());
   }

    private double[] rotatePeriods(double[] periods, int firstPeriod) {
        double[] result = new double[periods.length];

        for (int i = 0; i < periods.length; i++) {
            result[i] = periods[(i + firstPeriod)%periods.length];
        }

        return result;
    }

    @Override
    protected TripleExState state() {
        return state;
    }

    @Override
    protected void updateState(DataPoint point) {
        double oldLevel = state.level;
        double oldSlope = state.slope;
        int periodOfPoint = periodIndex(point.getTimestamp());

        state.level = levelSmoothing*(point.getValue() - state.periods[periodOfPoint]) +
                (1 - levelSmoothing)*(state.level + state.slope);
        state.slope = trendSmoothing*(state.level - oldLevel) + (1 - trendSmoothing)*state.slope;
        state.periods[periodOfPoint] = seasonalSmoothing*(point.getValue() - oldLevel - oldSlope) +
                (1 - seasonalSmoothing)*state.periods[periodOfPoint];
    }

    @Override
    protected PredictionResult calculatePrediction(int nAhead, Long learnTimestamp, Double expected) {

        /**
         * when learnTimestamp != null I want to get the prediction for that period
         */
        long predictTimestamp = learnTimestamp != null ? learnTimestamp :
                lastTimestamp + nAhead*metricContext.getCollectionInterval();

        int periodIndex = periodIndex(predictTimestamp);

        double prediction = state.level + nAhead*state.slope + state.periods[periodIndex];
        PredictionResult predictionResult = new PredictionResult(prediction);

        if (expected != null) {
            predictionResult.error = expected - predictionResult.value;
            residuals[periodIndex].add(predictionResult.error);
        }

        if (learnTimestamp == null) {
            predictionResult.sdOfResiduals = Utils.standardDeviation(residuals[periodIndex].toArray(new Double[0]));
        }

        return predictionResult;
    }

    private int periodIndex(long timestamp) {
        long difference = Math.abs(timestamp - state.firstTimestamp);
        long numberOfPeriodsAheadSinceLastLearn = difference/metricContext.getCollectionInterval();

        return (int)(numberOfPeriodsAheadSinceLastLearn%periods);
    }

    @Override
    public String toString() {
        return "TripleExponentialSmoothing{" +
                "levelSmoothing=" + levelSmoothing +
                ", trendSmoothing=" + trendSmoothing +
                ", seasonalSmoothing=" + seasonalSmoothing +
                ", level=" + state.level +
                ", slope=" + state.slope +
                ", periods=" + state.periods.length +
                ", periodsIndices=" + Arrays.toString(state.periods) +
                '}';
    }

    public static TripleExOptimizer optimizer(int periods) {
        return optimizer(periods, new ImmutableMetricContext(null, null, 1L));
    }

    public static TripleExOptimizer optimizer(MetricContext metricContext) {
        return new TripleExOptimizer(metricContext);
    }

    public static TripleExOptimizer optimizer(int periods, MetricContext metricContext) {
        return new TripleExOptimizer(periods, metricContext);
    }


    public static class TripleExOptimizer extends AbstractModelOptimizer {

        private Integer periods;

        private TripleExState initState;

        public TripleExOptimizer() {
            this(null, new ImmutableMetricContext(null, null, 1L));
        }

        public TripleExOptimizer(MetricContext metricContext) {
            this(null, metricContext);
        }

        public TripleExOptimizer(Integer periods, MetricContext metricContext) {
            super(metricContext);
            this.periods = periods;
        }

        public void setPeriods(Integer periods) {
            this.periods = periods;
        }

        public Integer getPeriods() {
            return periods;
        }

        @Override
        public TripleExponentialSmoothing minimizedMSE(List<DataPoint> dataPoints) {
            periods = periods == null ? AutomaticPeriodIdentification.periods(dataPoints) : periods;
            initState = TripleExponentialSmoothing.initState(dataPoints, periods, getMetricContext());

            try {
                double[] initialGuess = initialGuess(initState, periods);
                optimize(initialGuess, costFunction(dataPoints, periods));
            } catch (MathIllegalStateException ex) {
                // optimize without seasons
                log.error("Triple exponential smoothing optimizer failed to optimize periods", ex);
                periods = 0;
                double[] initialGuess = initialGuess(initState, periods);
                optimize(initialGuess, costFunction(dataPoints, periods));
            }

            log.debug("Triple ES: Optimizer best alpha: {}, beta {}, gamma {]",
                    this.result[0], this.result[1], this.result[2]);

            TripleExponentialSmoothing bestModel = model(result, periods, dataPoints.get(0).getTimestamp());
            bestModel.init(dataPoints);

            return bestModel;
        }

        private MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints,
                                                                final int periodsToOptimize) {
            // func for minimization
            MultivariateFunction multivariateFunction = new MultivariateFunction() {
                @Override
                public double value(double[] point) {
                    if (point[1] >= point[0]) {
                        return Double.POSITIVE_INFINITY;
                    }

                    TripleExponentialSmoothing tripleExponentialSmoothing = model(point, periodsToOptimize,
                            dataPoints.get(0).getTimestamp());
                    AccuracyStatistics accuracyStatistics = tripleExponentialSmoothing.init(dataPoints);

                    return accuracyStatistics.getMse();
                }
            };

            double[][] minMax = parametersMinMax(periodsToOptimize);
            return new MultivariateFunctionMappingAdapter(multivariateFunction, minMax[0], minMax[1]);
        }

        private double[] initialGuess(TripleExState state, int periodsToOptimize) {
            double[] initialGuess = new double[5 + periodsToOptimize];
            initialGuess[0] = DEFAULT_LEVEL_SMOOTHING;
            initialGuess[1] = DEFAULT_TREND_SMOOTHING;
            initialGuess[2] = DEFAULT_SEASONAL_SMOOTHING;
            initialGuess[3] = state.level;
            initialGuess[4] = state.slope;

            for (int i = 5; i < 5 + periodsToOptimize; i++) {
                initialGuess[i] = state.periods[i - 5];
            }

            return initialGuess;
        }

        private double[][] parametersMinMax(int periodsToOptimize) {
            double[] min = new double[5 + periodsToOptimize];
            double[] max = new double[5 + periodsToOptimize];
            min[0] = 0.0001;
            min[1] = 0.0001;
            min[2] = 0.0001;
            min[3] = Double.NEGATIVE_INFINITY;
            min[4] = Double.NEGATIVE_INFINITY;

            max[0] = 0.9999;
            max[1] = 0.9999;
            max[2] = 0.9999;
            max[3] = Double.POSITIVE_INFINITY;
            max[4] = Double.POSITIVE_INFINITY;

            for (int i = 5; i < 5 + periodsToOptimize; i++) {
                min[i] = Double.NEGATIVE_INFINITY;
                max[i] = Double.POSITIVE_INFINITY;
            }

            return new double[][]{min, max};
        }

        private TripleExponentialSmoothing model(double[] point, int periodsToOptimize, long firstTimestamp) {

            double alpha = point[0];
            double beta = point[1];
            double gamma = point[2];

            double level = point[3];
            double slope = point[4];

            double[] periods = new double[this.periods];
            for (int i = 0; i < this.periods; i++) {
                if (i < periodsToOptimize) {
                    periods[i] = point[5 + i];
                } else {
                    periods[i] = initState.periods[i];
                }
            }

            TripleExState state = new TripleExState(level, slope, periods, firstTimestamp);
            return TripleExponentialSmoothing.createWithState(state, alpha, beta, gamma, getMetricContext());
        }
    }
}
