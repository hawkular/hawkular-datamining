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

package org.hawkular.datamining.forecast.models.performance;

import java.io.IOException;

import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.hawkular.datamining.forecast.MetricContext;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;

/**
 * @author Pavol Loffay
 */
public class AbstractPerformanceTest {

    protected ModelData modelData;
    protected MetricContext metricContext;
    {
        try {
            int numberOfObservations = 250;
            modelData = ModelReader.read("sineLowVarLong");
            modelData.setData(modelData.getData().subList(0, numberOfObservations));

            metricContext = new ImmutableMetricContext(null, modelData.getName(), 1L);
        } catch (IOException ex) {
        }
    }
}
