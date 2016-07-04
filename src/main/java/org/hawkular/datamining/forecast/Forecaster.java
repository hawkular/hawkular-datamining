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

import java.util.List;

import org.hawkular.datamining.forecast.models.Model;
import org.hawkular.datamining.forecast.models.TimeSeriesModel;
import org.hawkular.datamining.forecast.stats.InformationCriterion;

/**
 * @author Pavol Loffay
 */
public interface Forecaster {

    /**
     * @see TimeSeriesModel#learn(DataPoint)
     */
    void learn(DataPoint dataPoint);

    /**
     * @see TimeSeriesModel#learn(List)
     */
    void learn(List<DataPoint> dataPoints);

    /**
     * @see TimeSeriesModel#forecast()
     */
    DataPoint forecast();

    /**
     * @see TimeSeriesModel#forecast(int)
     */
    List<DataPoint> forecast(int nAhead);

    /**
     * @return currently used model
     */
    TimeSeriesModel model();

    /**
     * @return information about metric
     */
    MetricContext context();

    /**
     * @return true if model is selected is initialized
     */
    boolean initialized();

    /**
     * @return last learned timestamp
     */
    long lastTimestamp();

    void update(Update update);

    Config config();


    class Config {
        private static int DEFAULT_WINDOW_SIZE = 60;
        private static AutomaticForecaster.ConceptDriftStrategy DEFAULT_CONCEPT_DRIFT_STRATEGY =
                new AutomaticForecaster.PeriodicIntervalStrategy(25);
        private static InformationCriterion DEFAULT_IC = InformationCriterion.AICc;

        private int windowsSize;
        private Model modelToUse;
        private InformationCriterion ic;
        private AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy;
        private Integer period;

        public Config(int windowsSize, Model modelToUse, InformationCriterion ic,
                      AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy, Integer period) {
            if (windowsSize <= 5 || ic == null || conceptDriftStrategy == null) {
                throw new IllegalArgumentException("Wrong forecaster configuration");
            }

            this.windowsSize = windowsSize;
            this.modelToUse = modelToUse;
            this.ic = ic;
            this.conceptDriftStrategy = conceptDriftStrategy;
            this.period = period;
        }

        public void update() {

        }

        public int getWindowsSize() {
            return windowsSize;
        }

        public Model getModelToUse() {
            return modelToUse;
        }

        public InformationCriterion getIc() {
            return ic;
        }

        public AutomaticForecaster.ConceptDriftStrategy getConceptDriftStrategy() {
            return conceptDriftStrategy;
        }

        public void setWindowsSize(int windowsSize) {
            this.windowsSize = windowsSize;
        }

        public void setModelToUse(Model modelToUse) {
            this.modelToUse = modelToUse;
        }

        public void setIc(InformationCriterion ic) {
            this.ic = ic;
        }

        public void setConceptDriftStrategy(
                AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy) {
            this.conceptDriftStrategy = conceptDriftStrategy;
        }

        public Integer getPeriod() {
            return period;
        }

        public void setPeriod(Integer period) {
            this.period = period;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Config getDefault() {
            return new Config(DEFAULT_WINDOW_SIZE, null, DEFAULT_IC, DEFAULT_CONCEPT_DRIFT_STRATEGY, null);
        }

        public void update(Update update) {
            if (update.getWindowSize() != null) {
                windowsSize = update.getWindowSize();
            }
            if (update.getModelToUse() != null) {
                modelToUse = update.getModelToUse();
            }
            if (update.getConceptDriftStrategy() != null) {
                conceptDriftStrategy = update.conceptDriftStrategy;
            }
            if (update.getIc() != null) {
                ic = update.getIc();
            }

            period = update.period;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Config)) return false;

            Config config = (Config) o;

            if (windowsSize != config.windowsSize) return false;
            if (modelToUse != config.modelToUse) return false;
            if (ic != config.ic) return false;
            if (period != config.period) return false;
            return !(conceptDriftStrategy != null ? !conceptDriftStrategy.equals(config.conceptDriftStrategy) :
                    config.conceptDriftStrategy != null);
        }

        @Override
        public int hashCode() {
            int result = windowsSize;
            result = 31 * result + (modelToUse != null ? modelToUse.hashCode() : 0);
            result = 31 * result + (ic != null ? ic.hashCode() : 0);
            result = 31 * result + (conceptDriftStrategy != null ? conceptDriftStrategy.hashCode() : 0);
            return result;
        }

        public static class Builder {
            private int windowSize = DEFAULT_WINDOW_SIZE;
            private InformationCriterion ic = DEFAULT_IC;
            private AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy = DEFAULT_CONCEPT_DRIFT_STRATEGY;
            private Model modelToUse;
            private Integer period;

            public Builder withWindowSize(int windowSize) {
                this.windowSize = windowSize;
                return this;
            }

            public Builder withInformationCriterion(InformationCriterion ic) {
                this.ic = ic;
                return this;
            }

            public Builder withConceptDriftStrategy(AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy) {
                this.conceptDriftStrategy = conceptDriftStrategy;
                return this;
            }

            public Builder withModelToUser(Model modelToUse) {
                this.modelToUse = modelToUse;
                return this;
            }

            public Builder withPeriod(Integer period) {
                this.period = period;
                return this;
            }

            public Config build() {
                return new Config(windowSize, modelToUse, ic, conceptDriftStrategy, period);
            }
        }
    }


    class Update {
        private Integer windowSize;
        private InformationCriterion ic;
        private AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy;
        private Model modelToUse;
        private Integer period;

        public Update(Integer windowsSize, Model modelToUse, InformationCriterion ic,
                      AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy, Integer period) {
            this.windowSize = windowsSize;
            this.modelToUse = modelToUse;
            this.ic = ic;
            this.conceptDriftStrategy = conceptDriftStrategy;
            this.period = period;
        }

        public Integer getWindowSize() {
            return windowSize;
        }

        public InformationCriterion getIc() {
            return ic;
        }

        public AutomaticForecaster.ConceptDriftStrategy getConceptDriftStrategy() {
            return conceptDriftStrategy;
        }

        public Model getModelToUse() {
            return modelToUse;
        }

        public Integer getPeriod() {
            return period;
        }

        public static class Builder {
            private Integer windowSize = 50;
            private InformationCriterion ic;
            private AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy;
            private Model modelToUse;
            private Integer period;

            public Builder withWindowSize(Integer windowSize) {
                this.windowSize = windowSize;
                return this;
            }

            public Builder withInformationCriterion(InformationCriterion ic) {
                this.ic = ic;
                return this;
            }

            public Builder withConceptDriftStrategy(AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy) {
                this.conceptDriftStrategy = conceptDriftStrategy;
                return this;
            }

            public Builder withModelToUse(Model modelToUse) {
                this.modelToUse = modelToUse;
                return this;
            }

            public Builder withPeriod(Integer period) {
                this.period = period;
                return this;
            }

            public Update build() {
                return new Update(windowSize, modelToUse, ic, conceptDriftStrategy, period);
            }
        }
    }
}
