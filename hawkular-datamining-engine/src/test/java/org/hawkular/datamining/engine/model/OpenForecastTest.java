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

package org.hawkular.datamining.engine.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hawkular.datamining.api.model.DataPoint;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.Observation;
import net.sourceforge.openforecast.models.DoubleExponentialSmoothingModel;

/**
 * @author Pavol Loffay
 */
public class OpenForecastTest {

    private static List<DataPoint> metricData;

    @BeforeClass
    public static void init() throws IOException {
        metricData = CSVTimeSeriesReader.getData("ar2.csv");
    }

    @Test
    public void testOpenForecast() throws IOException {

        final List<DataPoint> testData = metricData.subList(0, 50);

        List<net.sourceforge.openforecast.DataPoint> observations = dataPointsToObservations(testData);
        DataSet dataSet = new DataSet("t", 1, observations);

        ForecastingModel bestModel = DoubleExponentialSmoothingModel.getBestFitModel(dataSet);
    }

    public List<net.sourceforge.openforecast.DataPoint> dataPointsToObservations(List<DataPoint> dataPoints) {

        List<net.sourceforge.openforecast.DataPoint> points = new ArrayList<>();

        int counter = 1;
        for (DataPoint dataPoint: dataPoints) {
            Observation observation = new Observation(dataPoint.getValue());
            observation.setIndependentValue("t", counter++);

            points.add(observation);
        }

        return points;
    }
}
