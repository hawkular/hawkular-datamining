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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.hawkular.datamining.forecast.models.r.ModelData;
import org.hawkular.datamining.forecast.models.r.ModelReader;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.Observation;
import net.sourceforge.openforecast.models.DoubleExponentialSmoothingModel;

/**
 * @author Pavol Loffay
 */
public class OpenForecastTest extends AbstractTest {

    private static ModelData rModel;

    @BeforeClass
    public static void init() throws IOException {
        rModel = ModelReader.read("trendStatUpwardLowVar");
    }

    @Test
    public void testOpenForecast() throws IOException {

        List<net.sourceforge.openforecast.DataPoint> observations = dataPointsToObservations(rModel.getData());
        DataSet dataSet = new DataSet("t", 2, observations);

        // NPE
//        ForecastingModel openModel = Forecaster.getBestForecast(dataSet);
        ForecastingModel bestModel = DoubleExponentialSmoothingModel.getBestFitModel(dataSet);

        AutomaticForecaster forecaster = new AutomaticForecaster(new ImmutableMetricContext("",
                rModel.getName(), 1L));
        forecaster.learn(rModel.getData());
        AccuracyStatistics initStatistics = forecaster.model().initStatistics();

        Assert.assertTrue(initStatistics.getMse() < bestModel.getMSE());
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
