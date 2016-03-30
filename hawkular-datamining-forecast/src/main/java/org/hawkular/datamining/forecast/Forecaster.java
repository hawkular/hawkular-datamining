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

import org.hawkular.datamining.forecast.models.TimeSeriesModel;

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
     *
     * @return information about metric
     */
    MetricContext context();

    /**
     *
     * @return
     */
    boolean initialized();
}
