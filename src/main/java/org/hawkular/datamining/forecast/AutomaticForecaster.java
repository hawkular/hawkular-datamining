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

package org.hawkular.datamining.forecast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hawkular.datamining.forecast.models.DoubleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.ModelOptimizer;
import org.hawkular.datamining.forecast.models.SimpleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.TimeSeriesModel;
import org.hawkular.datamining.forecast.models.TripleExponentialSmoothing;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.hawkular.datamining.forecast.stats.InformationCriterionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.EvictingQueue;

/**
 * Selects the best model for given data set.
 *
 * @author Pavol Loffay
 */
public class AutomaticForecaster implements Forecaster {
    
    private static final Logger log = LoggerFactory.getLogger(AutomaticForecaster.class);

    private long counter;
    private EvictingQueue<DataPoint> window;

    private TimeSeriesModel usedModel;
    private final List<Function<MetricContext, ModelOptimizer>> applicableModels;

    private Config config;
    private final MetricContext metricContext;

    private Object selectModelLock = new Object();


    public AutomaticForecaster(MetricContext context) {
        this(context, Config.getDefault());
    }

    public AutomaticForecaster(MetricContext context, Config config) {
        if (context == null ||
                context.getCollectionInterval() == null || context.getCollectionInterval() <= 0) {
            throw new IllegalArgumentException("Invalid context.");
        }

        this.config = config;
        this.metricContext = context;

        config.getConceptDriftStrategy().forecaster = this;
        
        this.applicableModels = new ArrayList<>();
        this.applicableModels.add(new Function<MetricContext, ModelOptimizer>() {
            @Override
            public ModelOptimizer apply(MetricContext input) {
                return SimpleExponentialSmoothing.optimizer(input);
            }
        });
        this.applicableModels.add(new Function<MetricContext, ModelOptimizer>() {
            @Override
            public ModelOptimizer apply(MetricContext input) {
                return DoubleExponentialSmoothing.optimizer(input);
            }
        });
        this.applicableModels.add(new Function<MetricContext, ModelOptimizer>() {
            @Override
            public ModelOptimizer apply(MetricContext input) {
                return TripleExponentialSmoothing.optimizer(input);
            }
        });

        this.window = EvictingQueue.create(config.getWindowsSize());
    }

    @Override
    public void learn(DataPoint dataPoint) {
        learn(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {

        // recalculate if model is null or periodically after X points
        if (usedModel == null || config.getConceptDriftStrategy().shouldSelectNewModel(dataPoints.size())) {
            selectBestModel(dataPoints);
        } else {
            usedModel.learn(dataPoints);
        }

        counter += dataPoints.size();
        window.addAll(dataPoints);
    }

    @Override
    public DataPoint forecast() {
        if (!initialized()) {
            throw new IllegalStateException("Model not initialized, window remaining capacity = " +
                    window.remainingCapacity());
        }

        return usedModel.forecast();
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        if (!initialized()) {
            throw new IllegalStateException("Model not initialized, window remaining capacity = " +
                    window.remainingCapacity());
        }
        return usedModel.forecast(nAhead);
    }

    @Override
    public TimeSeriesModel model() {
        return usedModel;
    }

    @Override
    public MetricContext context() {
        return metricContext;
    }

    @Override
    public boolean initialized() {
        return usedModel != null;
    }

    @Override
    public long lastTimestamp() {
        return usedModel != null ? usedModel.lastTimestamp() : 0;
    }

    @Override
    public void update(Update update) {
        synchronized (selectModelLock) {
            if (update.getWindowSize() != null && !update.getWindowSize().equals(config.getWindowsSize())) {
                EvictingQueue<DataPoint> newWindow = EvictingQueue.create(update.getWindowSize());
                newWindow.addAll(window);
                window = newWindow;
            }

            if (update.getConceptDriftStrategy() != null) {
                update.getConceptDriftStrategy().forecaster = this;
            }

            config.update(update);
            List<DataPoint> empty = new ArrayList<>();
            selectBestModel(empty);
        }
    }

    @Override
    public Config config() {
        return config;
    }

    private void selectBestModel(final List<DataPoint> dataPoints) {
        synchronized (selectModelLock) {
            final List<DataPoint> initPoints = new ArrayList<>();
            initPoints.addAll(window);
            initPoints.addAll(dataPoints);

            if (initPoints.isEmpty()) {
                return;
            }

            log.debug("Estimating best model for: {}, previous: {}", metricContext.getMetricId(), usedModel);

            TimeSeriesModel bestModel = null;
            ModelOptimizer bestOptimizer = null;
            double bestIC = Double.POSITIVE_INFINITY;

            for (Function<MetricContext, ModelOptimizer> modelOptimizerSupplier : applicableModels) {
                ModelOptimizer modelOptimizer = modelOptimizerSupplier.apply(metricContext);

                /**
                 * if model is defined skip others
                 */
                if (config.getModelToUse() != null && !config.getModelToUse().isOptimizedBy(modelOptimizer)) {
                    continue;
                }

                try {
                    if (modelOptimizer instanceof TripleExponentialSmoothing.TripleExOptimizer) {
                        ((TripleExponentialSmoothing.TripleExOptimizer) modelOptimizer).setPeriods(config.getPeriod());
                    }
                    TimeSeriesModel currentModel = modelOptimizer.minimizedMSE(initPoints);

                    AccuracyStatistics initStatistics = currentModel.initStatistics();

                    InformationCriterionHolder icHolder = new InformationCriterionHolder(initStatistics.getSse(),
                            currentModel.numberOfParams(), initPoints.size());

                    log.debug("Estimated currentModel: {}, data size: {}, init MSE: {}, {}",
                            currentModel.toString(), initPoints.size(), initStatistics.getMse(), icHolder);

                    double currentIc = icHolder.informationCriterion(config.getIc());
                    if (currentIc < bestIC) {
                        bestIC = currentIc;
                        bestModel = currentModel;
                        bestOptimizer = modelOptimizer;
                    }
                } catch (IllegalArgumentException ex) {
                    continue;
                }
            }

            if (bestModel != null) {
                /**
                 * increase windows size if the model is seasonal
                 */
                if (bestModel instanceof TripleExponentialSmoothing) {
                    Integer periods = ((TripleExponentialSmoothing.TripleExOptimizer) bestOptimizer).getPeriods();
                    config.setPeriod(periods);

                    if (config.getWindowsSize() < periods * 3) {
                        config.setWindowsSize(periods * 3);
                        EvictingQueue<DataPoint> newWindow = EvictingQueue.create(periods * 3);
                        newWindow.addAll(window);
                        window = newWindow;
                    }
                }

                if (config.getConceptDriftStrategy() instanceof ErrorChangeStrategy) {
                    ((ErrorChangeStrategy) config.getConceptDriftStrategy()).setError(bestModel.initStatistics());
                }

                usedModel = bestModel;
                counter = 0;

                log.debug("Best model for: {}, is {}, {}", metricContext.getMetricId(),
                        bestModel.getClass().getSimpleName(), bestModel.initStatistics());
            }
        }
    }


    /**
     * Strategy used for dealing with concept drift (statistical properties of modelled time series change over time)
     * For instance originally monotonic time series has changed to trend stationary or seasonal pattern has showed up.
     */
    public abstract static class ConceptDriftStrategy {

        protected AutomaticForecaster forecaster;

        /**
         * @param learnSize size of dataSet from learn method
         * @return true if model should be selected - concept drift is present
         */
        public abstract boolean shouldSelectNewModel(int learnSize);
    }

    /**
     * The best model is periodically selected after each x learned points.
     */
    public static class PeriodicIntervalStrategy extends ConceptDriftStrategy {

        private int period;

        /**
         * @param period number of learning points after which the best model is selected
         */
        public PeriodicIntervalStrategy(int period) {
            if (period < 1) {
                throw new IllegalArgumentException("Period should be > 1");
            }

            this.period = period;
        }

        @Override
        public boolean shouldSelectNewModel(int learnSize) {
            return (forecaster.counter + learnSize) >= period;
        }

        public int getPeriod() {
            return period;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PeriodicIntervalStrategy)) return false;

            PeriodicIntervalStrategy that = (PeriodicIntervalStrategy) o;

            return period == that.period;

        }

        @Override
        public int hashCode() {
            return period;
        }
    }

    /**
     * Changes model if run accuracy differs by X percent to init accuracy (MSE, MAE).
     */
    public static class ErrorChangeStrategy extends ConceptDriftStrategy {

        private final int percentageChange;
        private final Statistics statistics;

        private double initError;

        public ErrorChangeStrategy(int percentageChange, Statistics statistics) {
            if (percentageChange > 100 || percentageChange < 1) {
                throw new IllegalArgumentException("Change should be between 1-100");
            }

            this.percentageChange = percentageChange;
            this.statistics = statistics;
        }

        @Override
        public boolean shouldSelectNewModel(int learnSize) {
            double runStat = statistics(forecaster.model().runStatistics(), statistics);

            double perChange =((Math.abs(runStat - initError))/initError)*100;

            return perChange > percentageChange;
        }

        public int getPercentageChange() {
            return percentageChange;
        }

        public Statistics getStatistics() {
            return statistics;
        }

        public void setError(AccuracyStatistics accuracyStatistics) {
            initError = statistics(accuracyStatistics, statistics);
        }

        private double statistics(AccuracyStatistics accuracyStatistics, Statistics statistics) {

            double stat = 0;

            if (statistics == Statistics.MAE) {
                stat = accuracyStatistics.getMae();
            } else if (statistics == Statistics.MSE) {
                stat = accuracyStatistics.getMse();
            }

            return stat;
        }

        public enum Statistics {
            MAE,
            MSE
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ErrorChangeStrategy)) return false;

            ErrorChangeStrategy that = (ErrorChangeStrategy) o;

            if (percentageChange != that.percentageChange) return false;
            if (Double.compare(that.initError, initError) != 0) return false;
            if (statistics != that.statistics) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = percentageChange;
            result = 31 * result + (statistics != null ? statistics.hashCode() : 0);
            temp = Double.doubleToLongBits(initError);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
