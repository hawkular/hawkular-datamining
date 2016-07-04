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

import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.hawkular.datamining.forecast.MetricContext;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.hawkular.datamining.forecast.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.EvictingQueue;

/**
 * Double exponential smoothing (Holt's linear trend model)
 * Works well when data exhibits increasing or decreasing trend pattern.
 *
 * <p>
 * When smoothing parameters are smaller more weights are added to the observations from distant past - it makes it
 * model more robust.
 *
 * <p>
 * Equations:
 * <ul>
 *  <li> level<sub>t</sub> = alpha*y<sub>t</sub> + (1-alpha)*(level<sub>t-1</sub>+trend<sub>t-1</sub>) </li>
 *  <li> trend<sub>t</sub> = beta*(level<sub>t</sub>-level<sub>t-1</sub>) + (1-beta)*trend<sub>t-1</sub> </li>
 *  <li> forecast<sub>t+h</sub> = level<sub>t</sub> + h*trend<sub>t</sub> </li>
 * </ul>
 *
 * @author Pavol Loffay
 */
public class DoubleExponentialSmoothing extends AbstractExponentialSmoothing {
    
    private static final Logger log = LoggerFactory.getLogger(DoubleExponentialSmoothing.class);

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;
    public static final double MIN_LEVEL_TREND_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_TREND_SMOOTHING = 0.9999;

    private DoubleExState state;
    private final double levelSmoothing;
    private final double trendSmoothing;

    private EvictingQueue<Double> residuals;


    public static class DoubleExState extends SimpleExponentialSmoothing.State {
        protected double slope;

        public DoubleExState(double level, double slope) {
            super(level);
            this.slope = slope;
        }
    }

    private DoubleExponentialSmoothing(double levelSmoothing, double trendSmoothing, MetricContext metricContext) {
        super(metricContext);

        if (levelSmoothing < MIN_LEVEL_TREND_SMOOTHING || levelSmoothing > MAX_LEVEL_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Level parameter should be in interval 0-1");
        }
        if (trendSmoothing < MIN_LEVEL_TREND_SMOOTHING || trendSmoothing > MAX_LEVEL_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Trend parameter should be in 0-1");
        }

        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
        this.residuals = EvictingQueue.create(50);
    }

    public static DoubleExponentialSmoothing createDefault() {
        return new DoubleExponentialSmoothing(DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING,
                ImmutableMetricContext.getDefault());
    }

    public static DoubleExponentialSmoothing createWithMetric(MetricContext metricContext) {
        return new DoubleExponentialSmoothing(DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING, metricContext);
    }

    public static DoubleExponentialSmoothing createWithSmoothingParams(double levelSmoothing, double trendSmoothing) {
        return new DoubleExponentialSmoothing(levelSmoothing, trendSmoothing, ImmutableMetricContext.getDefault());
    }

    public static DoubleExponentialSmoothing createCustom(double levelSmoothing, double trendSmoothing,
                                                          MetricContext metricContext) {
        return new DoubleExponentialSmoothing(levelSmoothing, trendSmoothing, metricContext);
    }

    @Override
    public String name() {
        return "Double exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 4;
    }

    @Override
    public int minimumInitSize() {
        return 2;
    }

    @Override
    protected DoubleExState initState(List<DataPoint> initData) {

        if (initData.size() < minimumInitSize()) {
            throw new IllegalArgumentException("For init are required " + minimumInitSize() + " points.");
        }

        double level;
        double slope;

        if (initData.size() == 2) {
            DataPoint[] dataPoints = initData.toArray(new DataPoint[0]);
            level = dataPoints[0].getValue();
            slope = dataPoints[1].getValue() - dataPoints[0].getValue();
        } else {
            SimpleRegression regression = new SimpleRegression();
            
            for (DataPoint dataPoint : initData) {
                regression.addData(dataPoint.getTimestamp(), dataPoint.getValue());
            }
            
            level = regression.predict(initData.get(0).getTimestamp());
            slope = regression.getSlope();
        }

        return state =new DoubleExState(level, slope);
    }

    @Override
    protected DoubleExponentialSmoothing.DoubleExState state() {
        return state;
    }

    @Override
    protected void updateState(DataPoint dataPoint) {
        double oldLevel = state.level;
        state.level = levelSmoothing*dataPoint.getValue() + (1 - levelSmoothing)*(state.level + state.slope);
        state.slope = trendSmoothing*(state.level - oldLevel) + (1 - trendSmoothing)*(state.slope);
    }

    @Override
    protected PredictionResult calculatePrediction(int nAhead, Long learnTimestamp, Double expected) {

        double value = state.level + state.slope * nAhead;
        PredictionResult predictionResult = new PredictionResult(value);

        if (expected != null) {
            predictionResult.error = expected - predictionResult.value;
            residuals.add(predictionResult.error);
        }

        if (learnTimestamp == null) {
            predictionResult.sdOfResiduals = Utils.standardDeviation(residuals.toArray(new Double[0]));
        }

        return predictionResult;
    }

    public static DoubleExOptimizer optimizer() {
        return new DoubleExOptimizer(new ImmutableMetricContext(null, null, 1L));
    }

    public static DoubleExOptimizer optimizer(MetricContext metricContext) {
        return new DoubleExOptimizer(metricContext);
    }

    @Override
    public String toString() {
        return "DoubleExponentialSmoothing{" +
                "alpha=" + levelSmoothing +
                ", beta=" + trendSmoothing +
                ", level=" + state.level +
                ", slope=" + state.slope +
                '}';
    }

    public static class DoubleExOptimizer extends AbstractModelOptimizer {


        public DoubleExOptimizer(MetricContext metricContext) {
            super(metricContext);
        }

        @Override
        public DoubleExponentialSmoothing minimizedMSE(List<DataPoint> dataPoints) {

            if (dataPoints.isEmpty()) {
                return DoubleExponentialSmoothing.createDefault();
            }

            optimize(new double[]{DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING}, costFunction(dataPoints));
            log.debug("Double ES: Optimizer best alpha:{}, beta {}", result[0], result[1]);

            DoubleExponentialSmoothing bestModel = new DoubleExponentialSmoothing(result[0], result[1],
                    getMetricContext());
            bestModel.init(dataPoints);

            return bestModel;

        }

        public MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints) {
            // func for minimization

            MultivariateFunction multivariateFunction = new MultivariateFunction() {
                @Override
                public double value(double[] point) {
                    double alpha = point[0];
                    double beta = point[1];

                    if (beta > alpha) {
                        return Double.POSITIVE_INFINITY;
                    }

                    DoubleExponentialSmoothing doubleExponentialSmoothing = new DoubleExponentialSmoothing(alpha, beta,
                            getMetricContext());
                    AccuracyStatistics accuracyStatistics = doubleExponentialSmoothing.init(dataPoints);

                    return accuracyStatistics.getMse();
                }
            };

            return new MultivariateFunctionMappingAdapter(multivariateFunction,
                    new double[]{MIN_LEVEL_TREND_SMOOTHING, MIN_LEVEL_TREND_SMOOTHING},
                    new double[]{MAX_LEVEL_TREND_SMOOTHING, MAX_LEVEL_TREND_SMOOTHING});
        }
    }
}
