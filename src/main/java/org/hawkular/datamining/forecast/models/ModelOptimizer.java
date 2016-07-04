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

import org.hawkular.datamining.forecast.DataPoint;

/**
 * Finds best model for given data set. Time series model in general contains parameters which optimal values can be
 * chosen to get model which best fits the data.
 *
 * @author Pavol Loffay
 */
public interface ModelOptimizer {

    /**
     * @param trainData training data set
     * @return best model for given data set. Model is already learned up to last timestamp in trainData
     */
    TimeSeriesModel minimizedMSE(List<DataPoint> trainData);

    /**
     * @return optimized parameters of the model. Length of array depends on a model being optimized.
     */
    double[] result();
}
