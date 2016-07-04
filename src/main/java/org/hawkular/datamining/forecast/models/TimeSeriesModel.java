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

import java.util.Comparator;
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;

/**
 * Basic interface for time series model
 *
 * <p>
 * All learn methods collects one step ahead prediction error {@link TimeSeriesModel#runStatistics()}.
 *
 * @author Pavol Loffay
 */
public interface TimeSeriesModel {

    /**
     * Initialize model and return statistics - error of one step ahead prediction
     * @param learnData points to learn
     * @return statistics for learnData
     */
    AccuracyStatistics init(List<DataPoint> learnData);

    /**
     *  Learn one point
     * @param learnData point to learn
     */
    void learn(DataPoint learnData);

    /**
     * Learn multiple points
     * @param learnData points to learn
     */
    void learn(List<DataPoint> learnData);

    /**
     * One step ahead prediction
     * @return predicted point
     */
    DataPoint forecast();

    /**
     * Multi step ahead prediction
     * @param nAhead number of steps for forecasting
     * @return predicted points
     */
    List<DataPoint> forecast(int nAhead);

    /**
     * @return initial statistics calculated when calling {@link #initStatistics()}
     */
    AccuracyStatistics initStatistics();

    /**
     * @return statistics calculated on learning data
     */
    AccuracyStatistics runStatistics();

    /**
     * @return name of the implemented model
     */
    String name();

    /**
     * @return  number of parameters of the model, seasonal models include seasonal indices
     */
    int numberOfParams();

    /**
     * @return  number of parameters of the model, seasonal models include seasonal indices
     */
    int minimumInitSize();

    long lastTimestamp();


    class TimestampComparator implements Comparator<DataPoint> {

        @Override
        public int compare(DataPoint point1, DataPoint point2) {
            return point1.getTimestamp().compareTo(point2.getTimestamp());
        }
    }
}
